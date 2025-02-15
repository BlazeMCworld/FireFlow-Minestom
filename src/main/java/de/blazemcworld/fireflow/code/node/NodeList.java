package de.blazemcworld.fireflow.code.node;

import de.blazemcworld.fireflow.FireFlow;
import de.blazemcworld.fireflow.code.node.impl.condition.*;
import de.blazemcworld.fireflow.code.node.impl.dictionary.*;
import de.blazemcworld.fireflow.code.node.impl.entity.RemoveEntityNode;
import de.blazemcworld.fireflow.code.node.impl.entity.SpawnEntityNode;
import de.blazemcworld.fireflow.code.node.impl.entity.TeleportEntityNode;
import de.blazemcworld.fireflow.code.node.impl.event.CancelEventNode;
import de.blazemcworld.fireflow.code.node.impl.event.player.*;
import de.blazemcworld.fireflow.code.node.impl.event.space.OnChunkLoadNode;
import de.blazemcworld.fireflow.code.node.impl.flow.*;
import de.blazemcworld.fireflow.code.node.impl.item.*;
import de.blazemcworld.fireflow.code.node.impl.list.*;
import de.blazemcworld.fireflow.code.node.impl.number.*;
import de.blazemcworld.fireflow.code.node.impl.player.effect.*;
import de.blazemcworld.fireflow.code.node.impl.player.info.*;
import de.blazemcworld.fireflow.code.node.impl.position.FacingVectorNode;
import de.blazemcworld.fireflow.code.node.impl.position.PackPositionNode;
import de.blazemcworld.fireflow.code.node.impl.position.UnpackPositionNode;
import de.blazemcworld.fireflow.code.node.impl.string.CharacterAtNode;
import de.blazemcworld.fireflow.code.node.impl.string.CombineStringsNode;
import de.blazemcworld.fireflow.code.node.impl.string.StringLengthNode;
import de.blazemcworld.fireflow.code.node.impl.text.CombineTextsNode;
import de.blazemcworld.fireflow.code.node.impl.text.FormatToTextNode;
import de.blazemcworld.fireflow.code.node.impl.variable.*;
import de.blazemcworld.fireflow.code.node.impl.vector.*;
import de.blazemcworld.fireflow.code.node.impl.world.GetBlockNode;
import de.blazemcworld.fireflow.code.node.impl.world.SetBlockNode;
import de.blazemcworld.fireflow.code.node.impl.world.SetRegionNode;
import de.blazemcworld.fireflow.util.Translations;
import net.minestom.server.item.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class NodeList {

    public static Category root;

    public static void init() {
        root = new Category("root", null)
            .add(new Category("condition", Material.COMPARATOR)
                    .add(new ConditionAndNode())
                    .add(new ConditionalChoiceNode<>(null))
                    .add(new ConditionOrNode())
                    .add(new InvertConditionNode())
                    .add(new ValuesEqualNode<>(null))
            )
            .add(new Category("dictionary", Material.CHISELED_BOOKSHELF)
                    .add(new DictionaryGetNode<>(null, null))
                    .add(new DictionaryHasNode<>(null, null))
                    .add(new DictionaryKeysNode<>(null, null))
                    .add(new DictionaryPutNode<>(null, null))
                    .add(new DictionaryRemoveNode<>(null, null))
                    .add(new DictionarySizeNode<>(null, null))
            )
            .add(new Category("entity", Material.ZOMBIE_HEAD)
                    .add(new RemoveEntityNode())
                    .add(new SpawnEntityNode())
                    .add(new TeleportEntityNode())
            )
            .add(new Category("event", Material.OBSERVER)
                    .add(new CancelEventNode())
                    .add(new OnChunkLoadNode())
                    .add(new OnPlayerAttackPlayerNode())
                    .add(new OnPlayerChatNode())
                    .add(new OnPlayerClickBlockNode())
                    .add(new OnPlayerJoinNode())
                    .add(new OnPlayerStartFlyingNode())
                    .add(new OnPlayerStartGlidingNode())
                    .add(new OnPlayerStartSneakingNode())
                    .add(new OnPlayerStartSprintingNode())
                    .add(new OnPlayerStopFlyingNode())
                    .add(new OnPlayerStopGlidingNode())
                    .add(new OnPlayerStopSneakingNode())
                    .add(new OnPlayerStopSprintingNode())
                    .add(new OnPlayerUseItemNode())
            )
            .add(new Category("flow", Material.REPEATER)
                    .add(new IfNode())
                    .add(new ListForEachNode<>(null))
                    .add(new PauseThreadNode())
                    .add(new RepeatNode())
                    .add(new ScheduleNode())
                    .add(new WhileNode())
            )
            .add(new Category("item", Material.ITEM_FRAME)
                    .add(new ItemsEqualNode())
                    .add(new SetItemCountNode())
                    .add(new SetItemLoreNode())
                    .add(new SetItemMaterialNode())
                    .add(new SetItemNameNode())
            )
            .add(new Category("list", Material.BOOKSHELF)
                    .add(new CreateListNode<>(null))
                    .add(new GetListIndexNode<>(null))
                    .add(new IndexInListNode<>(null))
                    .add(new ListAppendNode<>(null))
                    .add(new ListContainsNode<>(null))
                    .add(new ListInsertNode<>(null))
                    .add(new ListLengthNode<>(null))
                    .add(new RemoveListIndexNode<>(null))
                    .add(new RemoveListValueNode<>(null))
                    .add(new SetListIndexNode<>(null))
            )
            .add(new Category("number", Material.CLOCK)
                    .add(new AbsoluteNumberNode())
                    .add(new AddNumbersNode())
                    .add(new BasicNoiseNode())
                    .add(new ClampNumberNode())
                    .add(new DivideNumbersNode())
                    .add(new GreaterEqualNode())
                    .add(new GreaterThanNode())
                    .add(new LessEqualNode())
                    .add(new LessThanNode())
                    .add(new ModuloNode())
                    .add(new MultiplyNumbersNode())
                    .add(new ParseNumberNode())
                    .add(new RandomNumberNode())
                    .add(new RemainderNode())
                    .add(new RoundNumberNode())
                    .add(new SetToExponentialNode())
                    .add(new SquareRootNode())
                    .add(new SubtractNumbersNode())
            )
            .add(new Category("player", Material.PLAYER_HEAD)
                    .add(new Category("effect", Material.STONE_SWORD)
                        .add(new ClearInventoryNode())
                        .add(new GivePlayerItemNode())
                        .add(new KillPlayerNode())
                        .add(new PlayerAnimationNode())
                        .add(new RespawnPlayerNode())
                        .add(new SendActionbarNode())
                        .add(new SendBlockChangeNode())
                        .add(new SendMessageNode())
                        .add(new SendParticleNode())
                        .add(new SendTitleNode())
                        .add(new SetAllowFlyingNode())
                        .add(new SetExperienceLevelNode())
                        .add(new SetExperiencePercentageNode())
                        .add(new SetGamemodeNode())
                        .add(new SetHeldSlotNode())
                        .add(new SetPlayerFlyingNode())
                        .add(new SetPlayerFoodNode())
                        .add(new SetPlayerHealthNode())
                        .add(new SetPlayerInvulnerableNode())
                        .add(new SetPlayerSaturationNode())
                        .add(new SetPlayerVelocityNode())
                        .add(new TakePlayerItemNode())
                        .add(new TeleportPlayerNode())
                    )
                    .add(new Category("info", Material.ENDER_PEARL)
                        .add(new GetExperienceLevelNode())
                        .add(new GetExperiencePercentageNode())
                        .add(new GetHeldSlotNode())
                        .add(new GetPlayerFoodNode())
                        .add(new GetPlayerHealthNode())
                        .add(new GetPlayerNameNode())
                        .add(new GetPlayerSaturationNode())
                        .add(new GetPlayerUUIDNode())
                        .add(new IsPlayerInvulnerableNode())
                        .add(new IsPlayerSneakingNode())
                        .add(new IsPlayingNode())
                        .add(new PlayerCanFlyNode())
                        .add(new PlayerFromNameNode())
                        .add(new PlayerFromUUIDNode())
                        .add(new PlayerIsFlyingNode())
                        .add(new PlayerListNode())
                        .add(new PlayerMainItemNode())
                    )
            )
            .add(new Category("position", Material.COMPASS)
                    .add(new FacingVectorNode())
                    .add(new PackPositionNode())
                    .add(new PlayerPositionNode())
                    .add(new UnpackPositionNode())
            )
            .add(new Category("string", Material.STRING)
                    .add(new CharacterAtNode())
                    .add(new CombineStringsNode())
                    .add(new StringLengthNode())
            )
            .add(new Category("vector", Material.ARROW)
                    .add(new AddVectorsNode())
                    .add(new GetVectorComponentNode())
                    .add(new PackVectorNode())
                    .add(new RoundVectorAxesNode())
                    .add(new SetVectorComponentNode())
                    .add(new SetVectorLengthNode())
                    .add(new UnpackVectorNode())
            )
            .add(new Category("text", Material.WRITABLE_BOOK)
                    .add(new CombineTextsNode())
                    .add(new FormatToTextNode())
            )
            .add(new Category("variable", Material.ENDER_CHEST)
                    .add(new CacheValueNode<>(null))
                    .add(new DecrementVariableNode())
                    .add(new GetVariableNode<>(null))
                    .add(new IncrementVariableNode())
                    .add(new SetVariableNode<>(null))
            )
            .add(new Category("world",Material.GRASS_BLOCK)
                    .add(new GetBlockNode())
                    .add(new SetBlockNode())
                    .add(new SetRegionNode())
            )
            .add(new Category("function", Material.COMMAND_BLOCK).markFunctions())
            .finish();

        FireFlow.LOGGER.info("Loaded " + root.collectNodes().size() + " node types");
    }

    public static class Category {
        public final String name;
        public final Material icon;

        public final List<Category> categories = new ArrayList<>();
        public final List<Node> nodes = new ArrayList<>();
        public boolean isFunctions = false;
        public Predicate<Node> filter;

        public Category(String id, Material icon) {
            name = Translations.get("category." + id);
            this.icon = icon;
        }

        public Category(Category copy) {
            name = copy.name;
            icon = copy.icon;
            isFunctions = copy.isFunctions;
        }

        public Category add(Node node) {
            nodes.add(node);
            return this;
        }

        public Category add(Category category) {
            categories.add(category);
            return this;
        }

        public Category finish() {
            for (Category category : categories) {
                category.finish();
            }
            categories.sort(Comparator.comparing(c -> c.name));
            nodes.sort(Comparator.comparing(Node::getTitle));
            return this;
        }

        public List<Node> collectNodes() {
            List<Node> list = new ArrayList<>(nodes);
            for (Category category : categories) {
                list.addAll(category.collectNodes());
            }
            return list;
        }

        public Category markFunctions() {
            isFunctions = true;
            return this;
        }

        public Category filtered(Predicate<Node> filter) {
            Category filtered = new Category(this);
            for (Node n : nodes) {
                if (filter.test(n)) filtered.add(n);
            }
            for (Category c : categories) {
                Category fc = c.filtered(filter);
                if (fc.isFunctions) {
                    fc.filter = filter;
                    filtered.categories.add(fc);
                    continue;
                }
                if (fc.nodes.isEmpty() && fc.categories.isEmpty()) continue;
                filtered.categories.add(fc);
            }
            return filtered;
        }
    }
}
