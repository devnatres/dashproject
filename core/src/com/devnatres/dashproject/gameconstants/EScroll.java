package com.devnatres.dashproject.gameconstants;

import com.badlogic.gdx.math.Vector2;
import com.devnatres.dashproject.resourcestore.HyperStore;
import com.devnatres.dashproject.scroll.Scroll;
import com.devnatres.dashproject.scroll.ScrollPlane;

/**
 * Prefabricated scrolls.<br>
 *     <br>
 * Created by DevNatres on 06/03/2015.
 */
public enum EScroll {
    SC00 {
        @Override
        public Scroll create(HyperStore hyperStore) {
            return null;
        }
    },
    SC01 {
        @Override
        public Scroll create(HyperStore hyperStore) {
            ScrollPlane groundScroll = new ScrollPlane(hyperStore.getTexture("backgrounds/bg_galaxy_a.png"),
                    new Vector2(0f, 0f),
                    new Vector2(0f, -0.001f),
                    .1f);

            ScrollPlane cloudScroll = new ScrollPlane(hyperStore.getTexture("backgrounds/bg_galaxy_b.png"),
                    new Vector2(0f, 0f),
                    new Vector2(0f, -0.002f),
                    .8f);

            return new Scroll(groundScroll, cloudScroll);
        }
    },
    SC02 {
        @Override
        public Scroll create(HyperStore hyperStore) {
            ScrollPlane groundScroll = new ScrollPlane(hyperStore.getTexture("backgrounds/bg_ground_a.png"),
                    new Vector2(0f, 0f),
                    new Vector2(0f, -0.001f),
                    .1f);

            /*ScrollPlane cloudScroll = new ScrollPlane(hyperStore.getTexture("backgrounds/bg_ground_b.png"),
                    new Vector2(0f, 0f),
                    new Vector2(0f, -0.002f),
                    .8f);

            return new Scroll(groundScroll, cloudScroll);*/

            return new Scroll(groundScroll);
        }
    },
    SC03 {
        @Override
        public Scroll create(HyperStore hyperStore) {
            ScrollPlane cloudScroll = new ScrollPlane(hyperStore.getTexture("backgrounds/bg_ground_b.png"),
                    new Vector2(0f, 0f),
                    new Vector2(0f, -0.002f),
                    .8f);

            return new Scroll(cloudScroll);
        }
    },
    ;
    abstract public Scroll create(HyperStore hyperStore);
}

