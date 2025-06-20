@Mixin(GuiScreen.class)
public abstract class MixinSignOpen {

    @Inject(method = "initGui", at = @At("HEAD"))
    private void bazaarutils$onInitGui(CallbackInfo ci) {
        if (this instanceof GuiEditSign) {
            GuiEditSign sign = (GuiEditSign) (Object) this;  // classic cast
            BazaarUtils.EVENT_BUS.post(new SignOpenEvent(sign));
        }
    }
}
