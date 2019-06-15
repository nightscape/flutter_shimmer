#import "FlutterShimmerPlugin.h"
#import <flutter_shimmer/flutter_shimmer-Swift.h>

@implementation FlutterShimmerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterShimmerPlugin registerWithRegistrar:registrar];
}
@end
