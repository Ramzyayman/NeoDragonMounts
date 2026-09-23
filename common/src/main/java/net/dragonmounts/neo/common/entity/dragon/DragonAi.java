package net.dragonmounts.neo.common.entity.dragon;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;

import java.util.HashSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.dragonmounts.neo.common.entity.ai.behavior.*;
import net.dragonmounts.neo.common.init.DMActivities;
import net.dragonmounts.neo.common.init.DMEntities;
import net.dragonmounts.neo.common.init.DMMemories;
import net.dragonmounts.neo.common.init.DMSensors;
import net.dragonmounts.neo.common.util.BrainUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;

public class DragonAi {
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super ServerDragonEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.HURT_BY,
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.NEAREST_ADULT,
            SensorType.NEAREST_PLAYERS,
            DMSensors.DRAGON_TARGETS,
            DMSensors.DRAGON_TEMPTATIONS,
            DMSensors.OWNER
    );
    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.TEMPTING_PLAYER,
            MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
            MemoryModuleType.IS_TEMPTED,
            MemoryModuleType.BREED_TARGET,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.IS_PANICKING,
            MemoryModuleType.NEAREST_VISIBLE_ADULT,
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN,
            MemoryModuleType.NEAREST_ATTACKABLE,
            DMMemories.IS_ORDERED_TO_SIT,
            DMMemories.IS_CONTROLLED,
            DMMemories.FOLLOWABLE_OWNER,
            DMMemories.DISABLED_FOLLOWING_OWNER
    );
    protected static final ImmutableList<Activity> ORDERED_ACTIVITIES = ImmutableList.of(
            DMActivities.CONTROLLED,
            DMActivities.SITTING
    );
    protected static final ImmutableList<Activity> PRIORITIZED_ACTIVITIES = ImmutableList.of(
            DMActivities.CONTROLLED,
            DMActivities.SITTING,
            Activity.FIGHT,
            Activity.IDLE
    );

    /// 26.1 moved activity declaration onto Brain.ActivitySupplier and deleted the imperative
    /// convenience overloads; only the four-argument addActivity survives. These two rebuild exactly
    /// what the removed overloads did - ascending priorities from `start`, and for the single-memory
    /// variant a VALUE_PRESENT condition plus erasing that memory when the activity stops.
    private static ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super ServerDragonEntity>>> priorities(
            int start, ImmutableList<? extends BehaviorControl<? super ServerDragonEntity>> tasks
    ) {
        var builder = ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super ServerDragonEntity>>>builder();
        int priority = start;
        for (var task : tasks) {
            builder.add(Pair.of(priority++, task));
        }
        return builder.build();
    }

    private static void addActivity(
            Brain<ServerDragonEntity> brain, Activity activity, int start,
            ImmutableList<? extends BehaviorControl<? super ServerDragonEntity>> tasks
    ) {
        brain.addActivity(activity, priorities(start, tasks), ImmutableSet.of(), new HashSet<>());
    }

    private static void addActivityAndRemoveMemoryWhenStopped(
            Brain<ServerDragonEntity> brain, Activity activity, int start,
            ImmutableList<? extends BehaviorControl<? super ServerDragonEntity>> tasks,
            MemoryModuleType<?> memory
    ) {
        brain.addActivity(
                activity, priorities(start, tasks),
                ImmutableSet.of(Pair.of(memory, MemoryStatus.VALUE_PRESENT)), ImmutableSet.of(memory)
        );
    }

    static void initCoreActivity(Brain<ServerDragonEntity> brain) {
        addActivity(brain, Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink()
        ));
    }

    static void initIdleActivity(Brain<ServerDragonEntity> brain) {
        addActivity(brain, Activity.IDLE, 10, ImmutableList.of(
                new Swim<>(0.8F),
                new AnimalMakeLoveEx(DMEntities.TAMEABLE_DRAGON.get(), 1.0F, 4, 6),
                new FollowTemptation(entity -> 1.25F, entity -> 3.0),
                new FollowOwner(1.0F, 14.0F, 10),
                StartAttacking.create(DragonAi::findNearestValidAttackTarget),
                SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
                BrainUtil.dispatch(
                        new TryFindGround<>(32, 48, 0.75F, DragonAi::shouldFollowOwner),
                        new RunOne<>(ImmutableList.of(
                                Pair.of(RandomStroll.stroll(0.75F), 3),
                                Pair.of(SetWalkTargetFromLookTarget.create(
                                        target -> !(target instanceof Player),
                                        target -> 0.75F,
                                        5
                                ), 2),
                                Pair.of(new DoNothing(40, 100), 5)
                        )),
                        (level, dragon) -> dragon.isFlying() || dragon.isOrderedToSit()
                )
        ));
    }

    static void initFightActivity(Brain<ServerDragonEntity> brain) {
        addActivityAndRemoveMemoryWhenStopped(brain, Activity.FIGHT, 10, ImmutableList.of(
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
                MeleeAttack.create(40),
                StopAttackingIfTargetInvalid.create(),
                EraseMemoryIf.create(BehaviorUtils::isBreeding, MemoryModuleType.ATTACK_TARGET)
        ), MemoryModuleType.ATTACK_TARGET);
    }

    static void initControlledActivity(Brain<ServerDragonEntity> brain) {
        addActivityAndRemoveMemoryWhenStopped(
                brain,
                DMActivities.CONTROLLED,
                0,
                ImmutableList.of(new ControlledByPlayer()),
                DMMemories.IS_CONTROLLED
        );
    }

    static void initSittingActivity(Brain<ServerDragonEntity> brain) {
        brain.addActivity(DMActivities.SITTING, ImmutableList.of(Pair.of(0, BrainUtil.dispatch(
                new SitWhenOrderedTo(),
                new TryFindGround<>(32, 48, 0.75F, dragon -> {
                    if (dragon.isOrderedToSit()) return false;
                    dragon.getBrain().eraseMemory(DMMemories.IS_ORDERED_TO_SIT);
                    return true;
                }),
                (level, dragon) -> dragon.onGround()
        ))), ImmutableSet.of(Pair.of(DMMemories.IS_ORDERED_TO_SIT, MemoryStatus.VALUE_PRESENT)), ImmutableSet.of());
    }

    public static Brain.Provider<ServerDragonEntity> brainProvider() {
        // 26.1 added a declarative ActivitySupplier to provider(); this mod still wires activities
        // imperatively in makeBrain, so it supplies none here.
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES, body -> java.util.List.of());
    }

    public static Brain<ServerDragonEntity> makeBrain(Brain<ServerDragonEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain);
        initControlledActivity(brain);
        initSittingActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    public static void tickBrain(ServerLevel level, ServerDragonEntity dragon) {
        var brain = dragon.getBrain();
        boolean shouldSit = brain.hasMemoryValue(DMMemories.IS_ORDERED_TO_SIT);
        if (!shouldSit && dragon.isOrderedToSit()) {
            brain.setMemory(DMMemories.IS_ORDERED_TO_SIT, Unit.INSTANCE);
            shouldSit = true;
        }
        if ((shouldSit || brain.hasMemoryValue(DMMemories.IS_CONTROLLED)) && !ORDERED_ACTIVITIES.contains(
                brain.getActiveNonCoreActivity().orElse(null)
        )) {
            brain.stopAll(level, dragon);
            brain.setActiveActivityToFirstValid(ORDERED_ACTIVITIES);
        }
        brain.tick(level, dragon);
        brain.setActiveActivityToFirstValid(PRIORITIZED_ACTIVITIES);
    }

    public static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel level, ServerDragonEntity dragon) {
        return BehaviorUtils.isBreeding(dragon) ? Optional.empty() : dragon.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE);
    }

    public static boolean shouldFollowOwner(ServerDragonEntity dragon) {
        var brain = dragon.getBrain();
        if (brain.hasMemoryValue(DMMemories.DISABLED_FOLLOWING_OWNER)) return false;
        var owner = brain.getMemory(DMMemories.FOLLOWABLE_OWNER).orElse(null);
        return owner != null && (!owner.onGround() || owner.getBoundingBox().maxY > dragon.getY()) && dragon.distanceToSqr(owner) < 400.0;
    }
}
