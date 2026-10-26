package it.marco.shulkerplanner.gametest;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.ShulkerPlanner;
import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.ProjectStorage;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.config.HudBoxView;
import it.marco.shulkerplanner.config.HudCountFormat;
import it.marco.shulkerplanner.gui.GuiHudEditor;
import it.marco.shulkerplanner.gui.GuiPlannerConfigs;
import it.marco.shulkerplanner.gui.PlannerGuis;
import it.marco.shulkerplanner.gui.PlannerTab;
import it.marco.shulkerplanner.plan.BoxProgress;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.PlannedStack;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.render.HudRenderer;
import it.marco.shulkerplanner.util.BoxItemFactory;
import it.marco.shulkerplanner.util.InventoryScanner;

/**
 * Runs the whole addon inside a real client, in two parts:
 * <ol>
 *     <li>Functional checks: the plan, the inventory detection (right and wrong contents, identical boxes),
 *     a real shulker box placed in the world and filled by the server while its screen is open, the packing
 *     assistant auto-marking it, export de-duplication and the Litematica GUI integration.</li>
 *     <li>Showcase screenshots of every feature, with a bigger material list.</li>
 * </ol>
 * Screenshots end up in build/run/clientGameTest/screenshots.
 */
public class ShulkerPlannerClientGameTest implements FabricClientGameTest
{
    private static final String PROJECT = "GameTest Castle";
    private static final String SHOWCASE = "Medieval Castle";

    @Override
    public void runTest(ClientGameTestContext ctx)
    {
        ctx.runOnClient(mc -> {
            for (Path dir : List.of(ProjectStorage.getProjectDirectory(), ProjectStorage.getExportDirectory()))
            {
                try (var files = Files.list(dir))
                {
                    for (Path file : files.toList())
                    {
                        Files.deleteIfExists(file);
                    }
                }
                catch (java.io.IOException ignore) {}
            }

            mc.options.guiScale().set(3);
            Configs.Generic.HUD_ENABLED.setBooleanValue(false);
        });

        ctx.getInput().resizeWindow(1920, 1080);

        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create())
        {
            sp.getServer().runCommand("time set 6000");
            ctx.waitTicks(40);

            this.functionalChecks(ctx, sp);
            this.placementSelectionChecks(ctx, sp);
            this.showcase(ctx, sp);

            log("ALL CHECKS PASSED");
        }
    }

    // ------------------------------------------------------------------ functional checks

    private void functionalChecks(ClientGameTestContext ctx, TestSingleplayerContext sp)
    {
        TestMaterialList list = ctx.computeOnClient(mc -> new TestMaterialList(PROJECT, List.of(
                entry("stone", 5000, 5000),
                entry("oak_planks", 2300, 1900),
                entry("glass", 900, 900),
                entry("quartz_block", 27 * 64, 27 * 64),
                entry("redstone", 150, 120),
                entry("torch", 200, 200),
                entry("ender_pearl", 40, 40),
                entry("white_bed", 4, 4),
                entry("water_bucket", 3, 3),
                entry("shulker_box", 2, 2))));

        ctx.runOnClient(mc -> PlannerGuis.openForMaterialList(list, null));
        ctx.waitTicks(10);

        ShulkerPlan plan = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getPlan());
        log("Plan: %d boxes, %d full, %d items, %d stacks, %d loose", plan.getBoxCount(), plan.getFullBoxCount(),
            plan.getTotalItems(), plan.getTotalStacks(), plan.getLooseItems().size());
        check(plan.getBoxCount() == 7, "expected 7 boxes, got " + plan.getBoxCount());
        check(plan.getLooseItems().size() == 1, "the shulker boxes should be loose");

        for (PlannedBox box : plan.getBoxes())
        {
            StringBuilder sb = new StringBuilder();
            box.getTotals().forEach((id, count) -> sb.append(id.replace("minecraft:", "")).append('=').append(count).append(' '));
            log("  Box #%d %s slots=%d: %s", box.getIndex(), box.isPure() ? "pure" : "mixed", box.getUsedSlots(), sb);
        }

        // --- Inventory detection: one correct stone box, and box #5 with a single item missing
        sp.getServer().runCommand("clear @a");
        ctx.waitTicks(5);

        ctx.runOnClient(mc -> {
            PlannerProject project = PlannerSession.INSTANCE.getProject();
            List<PlannedBox> boxes = PlannerSession.INSTANCE.getPlan().getBoxes();

            BoxItemFactory.giveToPlayer(BoxItemFactory.createBoxItem(project, boxes.get(0), boxes.size()));

            ItemStack wrong = BoxItemFactory.createBoxItem(project, boxes.get(4), boxes.size());
            List<ItemStack> contents = new ArrayList<>(wrong.get(DataComponents.CONTAINER).itemCopies().toList());

            for (int i = contents.size() - 1; i >= 0; --i)
            {
                if (contents.get(i).isEmpty() == false && InventoryScanner.getId(contents.get(i)).equals("minecraft:redstone"))
                {
                    contents.get(i).shrink(1);
                    break;
                }
            }

            wrong.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            BoxItemFactory.giveToPlayer(wrong);
        });
        ctx.waitTicks(45);

        ctx.runOnClient(mc -> {
            PlannerSession session = PlannerSession.INSTANCE;
            List<PlannedBox> boxes = session.getPlan().getBoxes();
            log("Inventory detection: box1=%s box2(identical to 1)=%s box5(one item missing)=%s ready=%d",
                session.isBoxReady(boxes.get(0)), session.isBoxReady(boxes.get(1)), session.isBoxReady(boxes.get(4)), session.getReadyBoxCount());
            check(session.isBoxReady(boxes.get(0)), "the correct stone box was not detected");
            check(session.isBoxReady(boxes.get(1)) == false, "one stone box must not mark both identical stone boxes");
            check(session.isBoxReady(boxes.get(4)) == false, "a box with wrong contents must not be detected");
            check(session.getReadyBoxCount() == 1, "exactly one box should be ready");
        });

        // --- A real shulker box in the world, filled by the server while the screen is open
        sp.getServer().runCommand("clear @a");
        // Wait for the cleared inventory to reach the client, the scan would mark the box again otherwise
        ctx.waitTicks(10);
        ctx.runOnClient(mc -> {
            PlannerSession.INSTANCE.getProject().clearDoneBoxes();
            PlannerSession.INSTANCE.onProjectChanged();
        });
        ctx.waitTicks(25);

        BlockPos pos = sp.getServer().computeOnServer(server -> {
            BlockPos p = server.getPlayerList().getPlayers().getFirst().blockPosition().offset(2, 0, 0);
            command(server, "setblock " + p.getX() + " " + p.getY() + " " + p.getZ() + " purple_shulker_box");
            return p;
        });
        ctx.waitTicks(5);

        openShulker(ctx, sp, pos);

        int target = ctx.computeOnClient(mc -> {
            PlannedBox current = PlannerSession.INSTANCE.getCurrentBox();
            return current != null ? current.getIndex() : -1;
        });
        log("Empty shulker opened, the assistant suggests box #%d", target);
        check(target == 1, "an empty box should suggest the first box that is not ready");

        // The contents of box #6 (mixed) in reversed slot order: the slot order must not matter
        List<ItemStack> box6 = ctx.computeOnClient(mc -> {
            List<ItemStack> stacks = new ArrayList<>();

            for (PlannedStack ps : PlannerSession.INSTANCE.getPlan().getBoxes().get(5).getSlots())
            {
                stacks.add(ps.createStack());
            }

            Collections.reverse(stacks);
            return stacks;
        });

        fillShulker(sp, pos, box6, 0, box6.size() / 2);
        ctx.waitTicks(10);

        boolean doneHalf = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getProject().isBoxDone(PlannerSession.INSTANCE.getPlan().getBoxes().get(5)));
        int targetHalf = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getCurrentBox().getIndex());
        log("Half filled: assistant target #%d, done=%s", targetHalf, doneHalf);
        check(targetHalf == 6, "the assistant should recognize the partially filled box #6");
        check(doneHalf == false, "a half filled box must not be marked as done");

        int halfCount = 0;
        for (int i = 0; i < box6.size() / 2; ++i)
        {
            halfCount += box6.get(i).getCount();
        }

        int[] live = ctx.computeOnClient(mc -> {
            BoxProgress p = PlannerSession.INSTANCE.getCurrentProgress();
            return new int[] { p.getBox().getIndex(), p.getInBox(), p.isContentsKnown() ? 1 : 0, p.getMissing() };
        });
        log("HUD live progress: box #%d, %d items inside (expected %d), known=%d, missing=%d", live[0], live[1], halfCount, live[2], live[3]);
        check(live[0] == 6 && live[1] == halfCount && live[2] == 1, "the HUD does not follow the contents of the open box");
        shot(ctx, "check_assistant_half");

        fillShulker(sp, pos, box6, box6.size() / 2, box6.size());
        ctx.waitTicks(10);

        boolean doneFull = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getProject().isBoxDone(PlannerSession.INSTANCE.getPlan().getBoxes().get(5)));
        log("Completely filled: done=%s", doneFull);
        check(doneFull, "the completed box was not auto-marked by the packing assistant");
        shot(ctx, "check_assistant_complete");

        // Re-opening the same complete box must not mark anything else
        ctx.runOnClient(mc -> mc.player.closeContainer());
        ctx.waitTicks(5);
        openShulker(ctx, sp, pos);

        int readyAfterReopen = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getReadyBoxCount());
        log("Ready boxes after re-opening the complete box: %d", readyAfterReopen);
        check(readyAfterReopen == 1, "re-opening a complete box changed the done count");
        ctx.runOnClient(mc -> mc.player.closeContainer());

        // --- A partially filled box carried in the inventory is shown on the HUD
        sp.getServer().runCommand("clear @a");
        ctx.runOnClient(mc -> {
            PlannerProject project = PlannerSession.INSTANCE.getProject();
            PlannerSession.INSTANCE.setFocusedBox(2);
            List<PlannedBox> boxes = PlannerSession.INSTANCE.getPlan().getBoxes();
            ItemStack partial = BoxItemFactory.createBoxItem(project, boxes.get(2), boxes.size());
            List<ItemStack> half = new ArrayList<>();

            for (int i = 0; i < 10; ++i)
            {
                half.add(boxes.get(2).getSlots().get(i).createStack());
            }

            partial.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(half));
            BoxItemFactory.giveToPlayer(partial);
        });
        // Let the creative slot packet arrive first, the give command could otherwise use the same slot
        ctx.waitTicks(5);
        sp.getServer().runCommand("give @a minecraft:oak_planks 100");
        ctx.waitTicks(10);

        int[] carried = ctx.computeOnClient(mc -> {
            BoxProgress p = PlannerSession.INSTANCE.getCurrentProgress();
            return new int[] { p.getBox().getIndex(), p.getInBox(), p.getInInventory(), p.isContentsKnown() ? 1 : 0 };
        });
        log("Partial box in the inventory: box #%d, inside=%d, loose=%d, known=%d", carried[0], carried[1], carried[2], carried[3]);
        check(carried[0] == 3 && carried[1] == 640 && carried[2] == 100 && carried[3] == 1, "the partially filled box in the inventory is not shown");

        // --- The HUD can be dragged in the editor
        ctx.runOnClient(mc -> {
            Configs.Generic.HUD_ENABLED.setBooleanValue(true);
            Configs.Generic.HUD_X.setIntegerValue(4);
            Configs.Generic.HUD_Y.setIntegerValue(40);
            mc.gui.setScreen(new GuiHudEditor(null));
        });
        ctx.waitTicks(5);

        int[] moved = ctx.computeOnClient(mc -> {
            GuiHudEditor editor = (GuiHudEditor) mc.gui.screen();
            HudRenderer hud = HudRenderer.INSTANCE;
            int x = hud.getLastX() + 10;
            int y = hud.getLastY() + 10;
            editor.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(ScanCodes.OFFSET_MOUSE_LEFT, 0)), false);
            editor.mouseDragged(new MouseButtonEvent(x + 120, y + 30, new MouseButtonInfo(ScanCodes.OFFSET_MOUSE_LEFT, 0)), 120, 30);
            editor.mouseReleased(new MouseButtonEvent(x + 120, y + 30, new MouseButtonInfo(ScanCodes.OFFSET_MOUSE_LEFT, 0)));
            return new int[] { Configs.Generic.HUD_X.getIntegerValue(), Configs.Generic.HUD_Y.getIntegerValue() };
        });
        log("HUD dragged to %d, %d", moved[0], moved[1]);
        check(moved[0] == 124 && moved[1] == 70, "dragging the HUD did not move it");
        ctx.runOnClient(mc -> {
            Configs.Generic.HUD_X.setIntegerValue(4);
            Configs.Generic.HUD_Y.setIntegerValue(40);
            Configs.Generic.HUD_ENABLED.setBooleanValue(false);
            mc.gui.setScreen(null);
        });
        sp.getServer().runCommand("clear @a");

        // --- Export: always one single file per project
        int exports = ctx.computeOnClient(mc -> {
            PlannerProject project = PlannerSession.INSTANCE.getProject();

            for (int i = 0; i < 20; ++i)
            {
                ProjectStorage.writeExport(project.getName(), BoxItemFactory.createExportText(project, PlannerSession.INSTANCE.getPlan()));
            }

            try (var files = Files.list(ProjectStorage.getExportDirectory()))
            {
                return (int) files.count();
            }
            catch (java.io.IOException e)
            {
                return -1;
            }
        });
        log("Export files after 20 exports: %d", exports);
        check(exports == 1, "exporting must overwrite a single file");

        // --- Reminders can't be duplicated
        ctx.runOnClient(mc -> {
            PlannerProject project = PlannerSession.INSTANCE.getProject();
            project.addReminder(new Reminder("Build the stone farm", null));
            check(project.hasOpenReminder("build the stone farm "), "duplicate reminder not detected");
        });

        // --- Litematica GUI integration
        ctx.runOnClient(mc -> GuiBase.openGui(new GuiMaterialList(list)));
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> clickMalilibButton((GuiBase) mc.gui.screen(), "Shulker Planner"));
        ctx.waitTicks(5);
        check(isPlannerOpen(ctx), "the button in the material list did not open the planner");

        ctx.runOnClient(mc -> GuiBase.openGui(new GuiMainMenu()));
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> clickMalilibButton((GuiBase) mc.gui.screen(), "Shulker Planner"));
        ctx.waitTicks(5);
        check(isPlannerOpen(ctx), "the button in the Litematica main menu did not open the planner");

        sp.getServer().runCommand("clear @a");
        sp.getServer().runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " air");
        ctx.runOnClient(mc -> mc.gui.setScreen(null));
        ctx.waitTicks(5);
        log("Functional checks passed");
    }

    // ------------------------------------------------------------------ real Litematica placements

    /**
     * Two real schematics saved from the world, placed with Litematica: the planner and the HUD must always
     * follow the selected placement, and only show schematics that are loaded.
     */
    private void placementSelectionChecks(ClientGameTestContext ctx, TestSingleplayerContext sp)
    {
        BlockPos base = sp.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
        int x = base.getX() + 10;
        int y = base.getY();
        int z = base.getZ() + 10;

        // House A: stone bricks and glass. Tower B: oak planks and lanterns.
        sp.getServer().runCommand(String.format("fill %d %d %d %d %d %d stone_bricks", x, y, z, x + 4, y + 2, z + 4));
        sp.getServer().runCommand(String.format("fill %d %d %d %d %d %d glass", x + 1, y + 3, z + 1, x + 3, y + 3, z + 3));
        sp.getServer().runCommand(String.format("fill %d %d %d %d %d %d oak_planks", x + 10, y, z, x + 12, y + 5, z + 2));
        sp.getServer().runCommand(String.format("setblock %d %d %d lantern", x + 11, y + 6, z + 1));
        ctx.waitTicks(20);

        ctx.runOnClient(mc -> {
            var manager = DataManager.getSchematicPlacementManager();
            var houseA = createSchematic(mc, "House A", new BlockPos(x, y, z), new BlockPos(x + 4, y + 3, z + 4));
            var towerB = createSchematic(mc, "Tower B", new BlockPos(x + 10, y, z), new BlockPos(x + 12, y + 6, z + 2));
            var tower2 = createSchematic(mc, "Tower C", new BlockPos(x + 10, y, z), new BlockPos(x + 12, y + 6, z + 2));
            manager.addSchematicPlacement(SchematicPlacement.createFor(houseA, new BlockPos(x, y + 20, z), "House A", true, true), false);
            manager.addSchematicPlacement(SchematicPlacement.createFor(towerB, new BlockPos(x + 10, y + 20, z), "Tower B", true, true), false);
            manager.addSchematicPlacement(SchematicPlacement.createFor(tower2, new BlockPos(x + 20, y + 20, z), "Tower C", true, true), false);
            DataManager.setMaterialList(null);
        });

        String[] names = { "House A", "Tower B", "House A" };
        String[] expect = { "minecraft:stone_bricks", "minecraft:oak_planks", "minecraft:stone_bricks" };

        for (int i = 0; i < names.length; ++i)
        {
            String name = names[i];
            ctx.runOnClient(mc -> {
                selectPlacement(name);
                PlannerGuis.openPlanner(null, PlannerTab.MATERIALS);
            });
            ctx.waitFor(mc -> PlannerSession.INSTANCE.getProject() != null &&
                              PlannerSession.INSTANCE.getProject().getMaterials().isEmpty() == false &&
                              PlannerSession.INSTANCE.getProject().getName().equals(name), 400);
            String[] state = ctx.computeOnClient(mc -> {
                PlannerProject project = PlannerSession.INSTANCE.getProject();
                StringBuilder sb = new StringBuilder();
                project.getMaterials().forEach(r -> sb.append(r.getId()).append('=').append(r.getTotal()).append(' '));
                return new String[] { project.getName(), project.getSchematicName(), sb.toString(), String.valueOf(PlannerSession.INSTANCE.isLoadedInGame()) };
            });
            log("Selected '%s' -> project '%s', schematic '%s', loaded=%s, materials: %s", name, state[0], state[1], state[3], state[2]);
            check(state[0].equals(name), "the planner opened '" + state[0] + "' instead of the selected '" + name + "'");
            check(state[2].contains(expect[i]), "the materials are not the ones of '" + name + "'");
            check(state[3].equals("true"), "a selected placement must count as loaded");
            ctx.runOnClient(mc -> {
                mc.gui.setScreen(null);
                // Litematica remembers the first material list that was opened (this is what made the planner stick to one schematic)
                if (DataManager.getMaterialList() == null)
                {
                    DataManager.setMaterialList(PlannerSession.INSTANCE.getMaterialList());
                }
            });
        }

        // Selecting a placement with a plan switches the HUD by itself, without opening the planner
        ctx.runOnClient(mc -> selectPlacement("Tower B"));
        ctx.waitTicks(3);
        String followed = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getProject().getName());
        log("HUD follows the selection: %s", followed);
        check(followed.equals("Tower B"), "the HUD did not follow the selected placement");

        // A placement without a plan: the HUD shows the hint, not the previous schematic
        ctx.runOnClient(mc -> selectPlacement("Tower C"));
        ctx.waitTicks(3);
        String hint = ctx.computeOnClient(mc -> String.valueOf(PlannerSession.INSTANCE.getUnplannedSelection()));
        boolean shownC = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.isLoadedInGame());
        log("Unplanned selection: hint=%s, previous project shown=%s", hint, shownC);
        check(hint.equals("Tower C") && shownC == false, "an unplanned selection must not show another schematic on the HUD");

        // Unloading the schematic shown on the HUD: Litematica selects another placement and the HUD follows it
        ctx.runOnClient(mc -> {
            selectPlacement("Tower B");
            var manager = DataManager.getSchematicPlacementManager();

            for (SchematicPlacement placement : new ArrayList<>(manager.getAllSchematicsPlacements()))
            {
                if (placement.getName().equals("Tower B"))
                {
                    manager.removeSchematicPlacement(placement);
                }
            }
        });
        ctx.waitTicks(25);
        String afterUnload = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.getProject().getName() + " shown=" + PlannerSession.INSTANCE.isLoadedInGame());
        log("After unloading Tower B the HUD shows: %s", afterUnload);
        check(afterUnload.startsWith("Tower B") == false, "the HUD still shows the unloaded Tower B");

        // Nothing loaded at all: no HUD
        ctx.runOnClient(mc -> {
            var manager = DataManager.getSchematicPlacementManager();
            manager.setSelectedSchematicPlacement(null);

            for (SchematicPlacement placement : new ArrayList<>(manager.getAllSchematicsPlacements()))
            {
                manager.removeSchematicPlacement(placement);
            }

            DataManager.setMaterialList(null);
        });
        ctx.waitTicks(25);
        boolean shownNothingLoaded = ctx.computeOnClient(mc -> PlannerSession.INSTANCE.isLoadedInGame() || PlannerSession.INSTANCE.getUnplannedSelection() != null);
        log("HUD shown with no schematic loaded: %s", shownNothingLoaded);
        check(shownNothingLoaded == false, "the HUD is shown while no schematic is loaded");

        // The planner hotkey with nothing selected and nothing loaded must not reopen an old schematic
        boolean opened = ctx.computeOnClient(mc -> {
            PlannerGuis.openPlanner(null, null);
            return mc.gui.screen() != null && mc.gui.screen().getClass().getSimpleName().startsWith("GuiPlanner");
        });
        log("Planner opened with nothing loaded: %s", opened);
        check(opened == false, "the planner opened an unloaded schematic");
        ctx.waitTicks(5);
        sp.getServer().runCommand(String.format("fill %d %d %d %d %d %d air", x, y, z, x + 12, y + 6, z + 4));
        log("Placement selection checks passed");
    }

    private static LitematicaSchematic createSchematic(Minecraft mc, String name, BlockPos pos1, BlockPos pos2)
    {
        AreaSelection area = new AreaSelection();
        area.addSubRegionBox(new Box(pos1, pos2, name), false);
        LitematicaSchematic schematic = LitematicaSchematic.createFromWorld(mc.level, area,
                new LitematicaSchematic.SchematicSaveInfo(false, false), "MarcoTech81", str -> {});
        schematic.getMetadata().setName(name);
        return schematic;
    }

    private static void selectPlacement(String name)
    {
        var manager = DataManager.getSchematicPlacementManager();

        for (SchematicPlacement placement : manager.getAllSchematicsPlacements())
        {
            if (placement.getName().equals(name))
            {
                manager.setSelectedSchematicPlacement(placement);
                return;
            }
        }

        throw new AssertionError("No placement named " + name);
    }

    // ------------------------------------------------------------------ showcase screenshots

    private void showcase(ClientGameTestContext ctx, TestSingleplayerContext sp)
    {
        TestMaterialList list = ctx.computeOnClient(mc -> new TestMaterialList(SHOWCASE, List.of(
                entry("stone_bricks", 9200, 7400),
                entry("cobblestone", 4100, 3300),
                entry("stone_brick_stairs", 1300, 1100),
                entry("oak_planks", 2600, 2200),
                entry("spruce_log", 900, 900),
                entry("oak_stairs", 780, 610),
                entry("stone_brick_slab", 410, 410),
                entry("cobblestone_wall", 520, 380),
                entry("glass_pane", 640, 640),
                entry("iron_bars", 160, 160),
                entry("lantern", 120, 120),
                entry("torch", 180, 90),
                entry("spruce_trapdoor", 96, 96),
                entry("oak_door", 14, 14),
                entry("bookshelf", 64, 64),
                entry("ladder", 90, 90),
                entry("red_carpet", 230, 230),
                entry("hay_block", 40, 40),
                entry("white_banner", 12, 12),
                entry("campfire", 6, 6),
                entry("water_bucket", 2, 2),
                entry("shulker_box", 1, 1))));

        sp.getServer().runCommand("clear @a");

        // A little scene: a row of shulker boxes in front of the player
        sp.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            BlockPos base = player.blockPosition();
            String[] colors = { "purple", "magenta", "light_blue", "lime", "yellow", "orange", "red" };

            for (int i = 0; i < colors.length; ++i)
            {
                BlockPos p = base.offset(-3 + i, 0, 4);
                command(server, "setblock " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + colors[i] + "_shulker_box");
            }

            command(server, "tp @a " + (base.getX() + 0.5) + " " + base.getY() + " " + (base.getZ() + 0.5) + " 0 25");
        });

        ctx.runOnClient(mc -> {
            PlannerGuis.openForMaterialList(list, null);
            PlannerProject project = PlannerSession.INSTANCE.getProject();
            project.setGathered("minecraft:stone_bricks", true);
            project.setGathered("minecraft:cobblestone", true);
            project.setGathered("minecraft:glass_pane", true);
            project.setExcluded("minecraft:campfire", true);
            project.addReminder(new Reminder("Clear the area around the castle", null));
            project.addReminder(new Reminder("Gather 900 (14 x 64 + 4) Spruce Log", "minecraft:spruce_log"));
            project.addReminder(new Reminder("Craft the lanterns (iron nuggets!)", "minecraft:lantern"));
            Reminder done = new Reminder("Empty the ender chest", null);
            done.setDone(true);
            project.addReminder(done);
            PlannerSession.INSTANCE.onProjectChanged();
        });
        ctx.waitTicks(5);

        ctx.runOnClient(mc -> GuiBase.openGui(new GuiMaterialList(list)));
        shot(ctx, "01_litematica_material_list");

        ctx.runOnClient(mc -> GuiBase.openGui(PlannerGuis.create(PlannerTab.BOXES, null)));
        shot(ctx, "02_shulker_boxes");

        ctx.runOnClient(mc -> GuiBase.openGui(PlannerGuis.create(PlannerTab.MATERIALS, null)));
        shot(ctx, "03_materials");

        ctx.runOnClient(mc -> GuiBase.openGui(PlannerGuis.create(PlannerTab.REMINDERS, null)));
        shot(ctx, "04_reminders");

        // Creative: create the first boxes, then some loose items for the box shown on the HUD
        ctx.runOnClient(mc -> {
            List<PlannedBox> boxes = PlannerSession.INSTANCE.getPlan().getBoxes();
            PlannerProject project = PlannerSession.INSTANCE.getProject();

            for (int i = 0; i < 4; ++i)
            {
                BoxItemFactory.giveToPlayer(BoxItemFactory.createBoxItem(project, boxes.get(i), boxes.size()));
            }
        });
        ctx.waitTicks(30);

        int focus = ctx.computeOnClient(mc -> {
            int index = PlannerSession.INSTANCE.getPlan().getBoxes().size() - 3;
            PlannerSession.INSTANCE.setFocusedBox(index);
            return index;
        });

        // Part of what the followed box needs: complete, partial and missing items on the HUD
        sp.getServer().runCommand("give @a minecraft:ladder 90");
        sp.getServer().runCommand("give @a minecraft:glass_pane 300");
        ctx.waitTicks(30);

        ctx.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        shot(ctx, "05_inventory_created_boxes");

        ctx.runOnClient(mc -> GuiBase.openGui(PlannerGuis.create(PlannerTab.BOXES, null)));
        shot(ctx, "07_boxes_progress");

        // Packing assistant on a real placed shulker box, half filled and with a wrong item
        BlockPos pos = sp.getServer().computeOnServer(server -> {
            BlockPos p = server.getPlayerList().getPlayers().getFirst().blockPosition().offset(0, 0, 2);
            command(server, "setblock " + p.getX() + " " + p.getY() + " " + p.getZ() + " purple_shulker_box");
            return p;
        });
        ctx.waitTicks(5);

        List<ItemStack> focusBox = ctx.computeOnClient(mc -> {
            List<ItemStack> stacks = new ArrayList<>();

            for (PlannedStack ps : PlannerSession.INSTANCE.getPlan().getBoxes().get(focus).getSlots())
            {
                if (ps.id().equals("minecraft:lantern") == false)
                {
                    stacks.add(ps.createStack());
                }
            }

            return stacks;
        });

        fillShulker(sp, pos, focusBox, 0, focusBox.size() / 2);
        sp.getServer().runOnServer(server -> {
            ShulkerBoxBlockEntity be = (ShulkerBoxBlockEntity) server.getPlayerList().getPlayers().getFirst().level().getBlockEntity(pos);
            be.setItem(26, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:dirt")), 23));
        });
        openShulker(ctx, sp, pos);
        shot(ctx, "08_packing_assistant");
        ctx.runOnClient(mc -> mc.player.closeContainer());

        // The HUD keeps showing the box just filled, in the three views
        ctx.runOnClient(mc -> {
            Configs.Generic.HUD_ENABLED.setBooleanValue(true);
            Configs.Generic.HUD_BOX_VIEW.setOptionListValue(HudBoxView.BOTH);
            mc.gui.setScreen(null);
        });
        ctx.waitTicks(25);
        shot(ctx, "06_hud");

        ctx.runOnClient(mc -> Configs.Generic.HUD_BOX_VIEW.setOptionListValue(HudBoxView.SHULKER));
        shot(ctx, "06b_hud_shulker");

        ctx.runOnClient(mc -> {
            Configs.Generic.HUD_BOX_VIEW.setOptionListValue(HudBoxView.COMPACT);
            Configs.Generic.HUD_COUNT_FORMAT.setOptionListValue(HudCountFormat.ITEMS);
        });
        shot(ctx, "06c_hud_compact");

        ctx.runOnClient(mc -> {
            Configs.Generic.HUD_BOX_VIEW.setOptionListValue(HudBoxView.BOTH);
            Configs.Generic.HUD_COUNT_FORMAT.setOptionListValue(HudCountFormat.STACKS);
            mc.gui.setScreen(new GuiHudEditor(null));
        });
        shot(ctx, "11_hud_editor");
        ctx.runOnClient(mc -> mc.gui.setScreen(null));

        ctx.runOnClient(mc -> GuiBase.openGui(new GuiMainMenu()));
        shot(ctx, "09_litematica_main_menu");

        ctx.runOnClient(mc -> GuiBase.openGui(new GuiPlannerConfigs()));
        shot(ctx, "10_config");

        ctx.runOnClient(mc -> mc.gui.setScreen(null));
    }

    // ------------------------------------------------------------------ helpers

    private static void openShulker(ClientGameTestContext ctx, TestSingleplayerContext sp, BlockPos pos)
    {
        sp.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.openMenu((ShulkerBoxBlockEntity) player.level().getBlockEntity(pos));
        });
        ctx.waitForScreen(ShulkerBoxScreen.class);
        ctx.waitTicks(10);
    }

    private static void fillShulker(TestSingleplayerContext sp, BlockPos pos, List<ItemStack> stacks, int from, int to)
    {
        sp.getServer().runOnServer(server -> {
            ShulkerBoxBlockEntity be = (ShulkerBoxBlockEntity) server.getPlayerList().getPlayers().getFirst().level().getBlockEntity(pos);

            for (int i = from; i < to; ++i)
            {
                be.setItem(i, stacks.get(i).copy());
            }
        });
    }

    private static boolean isPlannerOpen(ClientGameTestContext ctx)
    {
        return ctx.computeOnClient(mc -> mc.gui.screen() != null && mc.gui.screen().getClass().getSimpleName().startsWith("GuiPlanner"));
    }

    private static void command(MinecraftServer server, String command)
    {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static void shot(ClientGameTestContext ctx, String name)
    {
        // Keep the cursor away from buttons, so no tooltips end up in the screenshots
        ctx.getInput().setCursorPos(1919, 1079);
        ctx.waitTicks(4);
        ctx.runOnClient(ShulkerPlannerClientGameTest::cleanScreen);
        ctx.waitTicks(2);
        ctx.takeScreenshot(name);
    }

    /**
     * Removes recipe toasts and chat messages from the screenshots
     */
    private static void cleanScreen(Minecraft mc)
    {
        mc.gui.toastManager().clear();
        mc.gui.hud.getChat().clearMessages(false);
    }

    private static MaterialListEntry entry(String id, int total, int missing)
    {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:" + id)));
        return new MaterialListEntry(stack, total, missing, 0, 0);
    }

    /**
     * The client game test API only finds vanilla widgets, malilib buttons are kept in their own list
     */
    @SuppressWarnings("unchecked")
    private static void clickMalilibButton(GuiBase gui, String label)
    {
        try
        {
            Field field = GuiBase.class.getDeclaredField("buttons");
            Field text = ButtonBase.class.getDeclaredField("displayString");
            field.setAccessible(true);
            text.setAccessible(true);

            for (ButtonBase button : (List<ButtonBase>) field.get(gui))
            {
                if (label.equals(text.get(button)))
                {
                    MouseButtonEvent click = new MouseButtonEvent(button.getX() + 2, button.getY() + 2,
                                                                  new MouseButtonInfo(ScanCodes.OFFSET_MOUSE_LEFT, 0));
                    button.onMouseClicked(click, false);
                    return;
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }

        throw new AssertionError("Button '" + label + "' not found");
    }

    private static void check(boolean condition, String message)
    {
        if (condition == false)
        {
            throw new AssertionError(message);
        }
    }

    private static void log(String fmt, Object... args)
    {
        ShulkerPlanner.LOGGER.info("[GameTest] " + String.format(fmt, args));
    }

    private static class TestMaterialList extends MaterialListBase
    {
        private final String name;

        private TestMaterialList(String name, List<MaterialListEntry> entries)
        {
            this.name = name;
            this.setMaterialListEntries(new ArrayList<>(entries));
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public String getTitle()
        {
            return "Material List for: " + this.name;
        }

        @Override
        public void reCreateMaterialList()
        {
            this.setMaterialListEntries(new ArrayList<>(this.getMaterialsAll()));
        }
    }
}
