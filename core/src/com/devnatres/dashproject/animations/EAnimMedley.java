package com.devnatres.dashproject.animations;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.devnatres.dashproject.dnagdx.DnaAnimation;
import com.devnatres.dashproject.resourcestore.HyperStore;

/**
 * Created by DevNatres on 01/05/2015.
 */
public enum EAnimMedley implements IAnimCreator {
    RADAR_RIGHT {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "radar_right.png", 1, 1, 8, Animation.PlayMode.LOOP_PINGPONG);
        }
    },
    RADAR_UP {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "radar_up.png", 1, 1, 8, Animation.PlayMode.LOOP_PINGPONG);
        }
    },
    RADAR_RIGHT_UP {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "radar_rightup.png", 1, 1, 8, Animation.PlayMode.LOOP_PINGPONG);
        }
    },

    TIME_HALO {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "time_halo.png", 3, 1, 10, Animation.PlayMode.LOOP_PINGPONG);
        }
    },

    NUMBERS_GOLD {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "numbers/numbers_gold.png", 1, 2, 10, Animation.PlayMode.LOOP_PINGPONG);
        }
    },

    DAMAGE_SOFT_FLASHING {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "damage_soft_flashing.png", 2, 1, 5, Animation.PlayMode.LOOP_PINGPONG);
        }
    },

    DAMAGE_HARD_FLASHING {
        public DnaAnimation create(HyperStore hyperStore) {
            return AnimTools.create(hyperStore, "damage_hard_flashing.png", 2, 1, 5, Animation.PlayMode.LOOP_PINGPONG);
        }
    },
}