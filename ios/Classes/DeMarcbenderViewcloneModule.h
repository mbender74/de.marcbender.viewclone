/**
 * ViewClone Module for Titanium iOS
 * Recursive deep cloning of Ti.UI.Views with all properties and child views
 *
 * Marc Bender
 * Licensed under the terms of the Apache Public License
 */

#import "TiModule.h"
#import "TiBase.h"
#import "TiProxy.h"
#import "TiViewProxy.h"

@interface DeMarcbenderViewcloneModule : TiModule {

}

- (id)cloneView:(id)args;

@end