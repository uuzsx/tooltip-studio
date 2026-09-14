package dev.tooltipstudio.smoke;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Actual game rendering for the Chinese one-page demo; not part of the release. */
final class PosterDemo extends Screen {
    private final ItemStack[] items;
    private final String[] labels = {"木棍", "钻石", "书", "苹果", "钻石剑", "下界之星"};
    private final String[] styles = {"legendary", "rare", "red_book", "uncommon", "cat_bell", "mythical"};
    private int frames;
    private volatile boolean saved;
    private SmokeClient.SlotHarness slots;

    PosterDemo() {
        super(Text.literal("Tooltip Studio item demo"));
        items = new ItemStack[]{
                make(Items.STICK, "传说木棍", "legendary", "普通木棍，也能使用传说样式", "分割线两端固定，中间拉伸"),
                make(Items.DIAMOND, "澄蓝晶石", "rare", "名字居中，说明文字左对齐", "贴图与布局可通过 JSON 修改"),
                make(Items.BOOK, "魔法手札", "red_book", "背景、边框和装饰放在一张图", "新增样式无需改 Java 代码"),
                make(Items.APPLE, "旅行口粮", "uncommon", "可按物品 ID 或标签匹配", "也可使用原版稀有度匹配"),
                make(Items.DIAMOND_SWORD, "月光长剑", "cat_bell", "用 NBT 为单件物品指定样式", "原有附魔与 lore 文字保留"),
                make(Items.NETHER_STAR, "夜幕星核", "mythical", "长文本换行后，名称仍居中", "JSON 与本地 PNG 支持重载")};
        try {
            StringNbtReader.parse("{TooltipStyle:\"legendary\",display:{Lore:['{\"text\":\"测试分割线\",\"italic\":false}']}}");
        } catch (Exception e) { throw new AssertionError("Poster example NBT is invalid", e); }
    }
    private static ItemStack make(Item item, String title, String style, String... lore) {
        ItemStack stack = new ItemStack(item);
        stack.setCustomName(Text.literal(title).styled(s -> s.withItalic(false)));
        stack.getOrCreateNbt().putString("TooltipStyle", style);
        stack.getOrCreateNbt().putInt("HideFlags", 127);
        NbtList lines = new NbtList();
        for (String line : lore) lines.add(NbtString.of(Text.Serialization.toJsonString(
                Text.literal(line).formatted(Formatting.GRAY).styled(s -> s.withItalic(false)))));
        stack.getOrCreateSubNbt("display").put("Lore", lines);
        return stack;
    }
    @Override protected void init() {
        slots = new SmokeClient.SlotHarness();
        slots.init(client, width, height);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xff17202e);
        for (int i = 0; i < items.length; i++) {
            int x = 15 + (i % 3) * 212;
            int y = 30 + (i / 3) * 165;
            context.drawItem(items[i], x, y);
            context.drawTextWithShadow(textRenderer, labels[i] + "  /  " + styles[i], x + 22, y + 5, 0xc8d8e8);
            slots.show(context, items[i], x - 9, y + 47);
        }
        context.draw();
        if (frames++ == 30) ScreenshotRecorder.saveScreenshot(client.runDirectory, "tooltip-studio-items-demo.png",
                client.getFramebuffer(), message -> saved = true);
        if (frames > 45 && saved) client.scheduleStop();
    }
    @Override public boolean shouldPause() { return false; }
}
