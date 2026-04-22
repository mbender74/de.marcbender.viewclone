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

#pragma mark Private

- (TiViewProxy *)cloneProxy:(TiViewProxy *)proxy
{
  NSDictionary *props = [proxy allProperties];
  NSDictionary *filteredProps = nil;

  if (props != nil && [props count] > 0) {
    NSMutableDictionary *mutableProps = [[NSMutableDictionary alloc] initWithCapacity:[props count]];
    [props enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
      if (![obj isKindOfClass:[NSNull class]]) {
        [mutableProps setObject:obj forKey:key];
      }
    }];
    filteredProps = mutableProps;
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
}

@end