package net.gudenau.cavegame.wooting.internal;

import net.gudenau.cavegame.wooting.HidKeyCodes;
import net.gudenau.cavegame.wooting.WootingAnalog;
import net.gudenau.cavegame.wooting.WootingCommon;

/**
 * A dummy class to make it so you can't implement public interfaces that are not meant to be implemented.
 */
public final class Dummy implements WootingCommon, WootingAnalog, HidKeyCodes {
    private Dummy() {
        throw new AssertionError();
    }
}
