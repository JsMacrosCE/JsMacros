// Compile against a generated target's headers. These checks must not execute in-game.
declare const ent: EntityHelper | null;
if (ent instanceof EntityHelpers.Horse) {
    const variant: number = ent.getVariant();
    const horse: EntityHelpers.Horse = ent;
    // @ts-expect-error the variant is not a string
    const wrong: string = ent.getVariant();
}
if (ent instanceof EntityHelpers.AbstractHorse) {
    const horse: EntityHelpers.AbstractHorse = ent;
}
// @ts-expect-error a namespace type member is read-only
EntityHelpers.Horse = EntityHelpers.Living;
// @ts-expect-error unpublished type names must not resolve
EntityHelpers.NotAnExport;

declare const inventory: Inventory;
if (inventory instanceof Inventories.Villager) inventory.getTrades();
declare const widget: ClickableWidgetHelper;
if (widget instanceof Widgets.Slider) widget.getValue();
declare const element: unknown;
if (element instanceof RenderElements.Text) element.setText("updated");
if (element instanceof RenderElements.Element) element.getZIndex();
const client = RenderElements.Element.mc;
// @ts-expect-error undocumented static members must not appear in the typings
RenderElements.Element.notAnExportedStaticMember();
declare const nbt: NBTElementHelper;
if (nbt instanceof NBTHelpers.Compound) nbt.getKeys();
declare const hit: HitResultHelper | null;
if (hit instanceof HitResults.Entity) hit.getEntity();
const filter = new ScannerFilters.Not(new ScannerFilters.StringCompare("EQUALS", "air"));
filter.apply("stone");
ScriptClasses.CustomImage.createWidget(16, 16, "badge");
