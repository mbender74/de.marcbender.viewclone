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
// Weak references allow GC to reclaim memory when needed
static NSMutableDictionary *gPropertyCache = nil;
static NSMutableDictionary *gClonedProxyMap = nil;
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
      gPropertyCache = [[NSMutableDictionary alloc] init];
      gClonedProxyMap = [[NSMutableDictionary alloc] init];
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
 */
- (void)clearCache
{
  @synchronized(gCacheLock) {
    if (gPropertyCache != nil) {
      [gPropertyCache removeAllObjects];
      DebugLog(@"[INFO] Property cache cleared");
    }
    if (gClonedProxyMap != nil) {
      [gClonedProxyMap removeAllObjects];
      DebugLog(@"[INFO] Cloned proxy map cleared");
    }
    if (gCloningInProgress != nil) {
      [gCloningInProgress removeAllObjects];
      DebugLog(@"[INFO] Cloning in-progress set cleared");
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
  @synchronized(gCacheLock) {
    if (gPropertyCache == nil) {
      return 0;
    }
    return (NSInteger)[gPropertyCache count];
  }
}

#pragma mark Private

// Cache for filtered properties - Reduces NSNull filtering overhead on repeated clones
- (NSMutableDictionary *)cachedFilteredPropsForProps:(NSDictionary *)props {
    @synchronized(gCacheLock) {
        // Check if cache is initialized
        if (gPropertyCache == nil) {
            gPropertyCache = [[NSMutableDictionary alloc] init];
        }

        NSString *key = [NSString stringWithFormat:"%p", props];
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

        return cached;
    }
}

- (TiViewProxy *)cloneProxy:(TiViewProxy *)proxy {
  // Zirkelbeziehungs-Erkennung - Vermeidet StackOverflow bei recursive references
  if (gCloningInProgress != nil && [gCloningInProgress containsObject:proxy]) {
    DebugLog(@"[WARN] Circular reference detected: %@ - skipping", NSStringFromClass([proxy class]));
    return nil;
  }

  // Zum Set hinzufügen - Markiert Proxy als "wird geklont"
  if (gCloningInProgress != nil) {
    [gCloningInProgress addObject:proxy];
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

    // Store cloned proxy for memory management tracking
    @synchronized(gCacheLock) {
        if (gClonedProxyMap == nil) {
            gClonedProxyMap = [[NSMutableDictionary alloc] init];
        }
        // Use weak reference to avoid retain cycle
        NSValue *proxyKey = [NSValue valueWithPointer:proxy];
        gClonedProxyMap[proxyKey] = clonedProxy;
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
    // Vom Set entfernen - Freigabe für zukünftige Clones
    if (gCloningInProgress != nil) {
      [gCloningInProgress removeObject:proxy];
    }
  }
}

@end
