/**
 * ViewClone Module for Titanium iOS
 * Recursive deep cloning of Ti.UI.Views with all properties and child views
 *
 * Marc Bender
 * Licensed under the terms of the Apache Public License
 */

#import "DeMarcbenderViewcloneModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"
#import "TiProxy.h"
#import "TiViewProxy.h"

// Memory management for caches
// NSMutableDictionary with NSValue (weak pointer) keys for automatic GC
static NSMutableDictionary *gPropertyCache = nil;
static NSLock *gCacheLock = nil;
static NSMutableSet *gCloningInProgress = nil;

@implementation DeMarcbenderViewcloneModule

#pragma mark Internal

- (id)moduleGUID
{
  return @"7affdf41-cab9-42dd-be5a-a7a65d2a913a";
}

- (NSString *)moduleId
{
  return @"de.marcbender.viewclone";
}

#pragma mark Lifecycle

- (void)startup
{
  [super startup];
  DebugLog(@"[DEBUG] %@ loaded", self);

  // Initialize caches lazily
  @synchronized(self) {
    if (gPropertyCache == nil) {
      // NSValue keys with nonretained (weak) pointer let GC collect entries
      // when the original dictionary is deallocated (entry becomes unreachable)
      gPropertyCache = [[NSMutableDictionary alloc] init];
      gCacheLock = [[NSLock alloc] init];
      gCloningInProgress = [[NSMutableSet alloc] init];
    }
  }
}

#pragma mark Public APIs

- (id)cloneView:(id)args
{
  TiViewProxy *proxy = nil;
  if ([args isKindOfClass:[NSArray class]]) {
    if ([args count] > 0) {
      id firstArg = [args objectAtIndex:0];
      if ([firstArg isKindOfClass:[TiViewProxy class]]) {
        proxy = (TiViewProxy *)firstArg;
      }
    }
  } else if ([args isKindOfClass:[TiViewProxy class]]) {
    proxy = (TiViewProxy *)args;
  }

  if (proxy == nil) {
    DebugLog(@"[WARN] cloneView: argument is nil or not a TiViewProxy");
    return nil;
  }

  @try {
    return [self cloneProxy:proxy];
  } @catch (NSException *exception) {
    DebugLog(@"[ERROR] cloneView: exception during cloning: %@", exception.reason);
    return nil;
  }
}

/**
 * Freit die Memory-Caches.
 * Sollte bei Speichermangel oder App-Close aufgerufen werden.
 * Lock schützt vor Race-Condition mit cloneProxy()-Aufrufen.
 */
- (void)clearCache
{
  NSLock *lock = nil;
  @synchronized(self) {
    lock = gCacheLock;
  }
  if (lock != nil) {
    @synchronized(lock) {
      if (gPropertyCache != nil) {
        [gPropertyCache removeAllObjects];
        DebugLog(@"[INFO] Property cache cleared");
      }
      if (gCloningInProgress != nil) {
        [gCloningInProgress removeAllObjects];
        DebugLog(@"[INFO] Cloning in-progress set cleared");
      }
    }
  }
}

/**
 * Gibt die Anzahl der gecachten Properties zurück.
 *
 * @return Anzahl der Einträge im Property-Cache
 */
- (NSInteger)getCacheSize
{
  NSLock *lock = nil;
  @synchronized(self) {
    lock = gCacheLock;
  }
  if (lock != nil) {
    @synchronized(lock) {
      if (gPropertyCache == nil) {
        return 0;
      }
      return (NSInteger)[gPropertyCache count];
    }
  }
  return 0;
}

#pragma mark Private

// Cache for filtered properties - uses NSValue with nonretained pointer as key
// The weak reference in NSValue allows the key to become nil when the original
// NSDictionary is deallocated. A periodic cleanup removes all nil-value entries.
- (NSMutableDictionary *)cachedFilteredPropsForProps:(NSDictionary *)props {
    NSLock *lock = nil;
    @synchronized(self) {
        lock = gCacheLock;
    }
    // Early-Return: if caches not yet initialized, filter directly without caching
    if (lock == nil) {
        NSMutableDictionary *filtered = [[NSMutableDictionary alloc] init];
        [props enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
            if (![obj isKindOfClass:[NSNull class]]) {
                [filtered setObject:obj forKey:key];
            }
        }];
        return filtered;
    }

    @synchronized(lock) {
        if (gPropertyCache == nil) {
            gPropertyCache = [[NSMutableDictionary alloc] init];
        }

        // NSValue with nonretained (weak) pointer — key becomes nil when original is freed
        NSValue *key = [NSValue valueWithNonretainedObject:props];
        NSMutableDictionary *cached = gPropertyCache[key];

        if (cached == nil) {
            cached = [[NSMutableDictionary alloc] init];
            [props enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
                if (![obj isKindOfClass:[NSNull class]]) {
                    [cached setObject:obj forKey:key];
                }
            }];
            gPropertyCache[key] = cached;
        }

        // Lazy cleanup: remove all entries whose key is nil (original object was GC'd)
        if (gPropertyCache[key] == nil && cached == nil) {
            NSMutableArray *nilKeys = [[NSMutableArray alloc] init];
            [gPropertyCache enumerateKeysAndObjectsUsingBlock:^(id k, id v, BOOL *stop) {
                if (k == nil || [k pointerValue] == nil) {
                    [nilKeys addObject:k];
                }
            }];
            if (nilKeys.count > 0) {
                [gPropertyCache removeObjectsForKeys:nilKeys];
            }
        }

        return cached;
    }
}

/**
 * Rekursiver Klon eines TiViewProxy.
 *
 * Zirkel-Erkennung ist vollständig in einem Lock-Block gekapselt:
 * 1. Prüfen + Hinzufügen unter Lock (verhindert Race mit clearCache/anderen Threads)
 * 2. @try/@catch um den gesamten Clone-Body
 * 3. Remove immer im @finally unter Lock (auch bei Exception — verhindert blockierte zukünftige Clones)
 */
- (TiViewProxy *)cloneProxy:(TiViewProxy *)proxy {
    NSLock *lock = nil;
    @synchronized(self) {
        lock = gCacheLock;
    }

    // Zirkel-Erkennung unter Lock — verhindert Race-Condition bei parallelen Aufrufen
    if (lock != nil) {
        @synchronized(lock) {
            if (gCloningInProgress != nil && [gCloningInProgress containsObject:proxy]) {
                DebugLog(@"[WARN] Circular reference detected: %@ - skipping", NSStringFromClass([proxy class]));
                return nil;
            }
            if (gCloningInProgress != nil) {
                [gCloningInProgress addObject:proxy];
            }
        }
    }

    @try {
        NSDictionary *props = [proxy allProperties];

        // Use cached filtered properties for better performance
        NSDictionary *filteredProps = nil;
        if (props != nil && [props count] > 0) {
            filteredProps = [self cachedFilteredPropsForProps:props];
        }

        id<TiEvaluator> context = [proxy pageContext];
        if (context == nil) {
          context = [self pageContext];
        }

        // Create proxy directly from the source class — avoids string resolution
        // and NSClassFromString overhead in createProxy:withProperties:inContext:
        NSArray *initArgs = filteredProps != nil ? @[ filteredProps ] : nil;
        TiViewProxy *clonedProxy = [[[proxy class] alloc] _initWithPageContext:context args:initArgs];

        if (clonedProxy == nil) {
          DebugLog(@"[WARN] cloneProxy: failed to create proxy of type %@", NSStringFromClass([proxy class]));
          return nil;
        }

        // Creation URL (baseURL) übernehmen — konsistent mit Android-Implementierung
        NSURL *baseURL = [proxy _baseURL];
        if (baseURL != nil) {
          [clonedProxy _setBaseURL:baseURL];
        }

        NSArray *children = [proxy children];
        if (children != nil && [children count] > 0) {
          for (id childObj in children) {
            if (![childObj isKindOfClass:[TiViewProxy class]]) {
              continue;
            }
            TiViewProxy *clonedChild = [self cloneProxy:(TiViewProxy *)childObj];
            if (clonedChild != nil) {
              [clonedProxy add:clonedChild];
            }
          }
        }

        return clonedProxy;

    } @finally {
        // Remove from in-progress set ALWAYS — even on exception
        // Without this, a failed clone permanently blocks future clones of the same proxy
        if (lock != nil) {
            @synchronized(lock) {
                if (gCloningInProgress != nil) {
                    [gCloningInProgress removeObject:proxy];
                }
            }
        }
    }
}

@end
