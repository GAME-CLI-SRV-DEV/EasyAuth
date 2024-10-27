package xyz.nikitacartes.easyauth.utils;

import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * PLayer authentication extension.
 */
public interface PlayerAuth {
    void easyAuth$saveLastLocation(boolean saveDimension);

    void easyAuth$saveLastDimension(RegistryKey<World> registryKey);

    void easyAuth$restoreLastLocation();

    /**
     * Converts player uuid, to ensure player with "nAmE" and "NamE" get same uuid.
     * Both players are not allowed to play, since mod mimics Mojang behaviour.
     * of not allowing accounts with same names but different capitalization.
     *
     * @return converted UUID as string
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    String easyAuth$getFakeUuid();


    /**
     * Sets the authentication status of the player.
     *
     * @param authenticated whether player should be authenticated
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    void easyAuth$setAuthenticated(boolean authenticated);

    /**
     * Checks whether player is authenticated.
     *
     * @return false if player is not authenticated, otherwise true.
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    boolean easyAuth$isAuthenticated();

    /**
     * Gets the text which tells the player
     * to login or register, depending on account status.
     *
     * @return Text with appropriate string (login or register)
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    void easyAuth$sendAuthMessage();

    /**
     * Checks whether player is a fake player (from CarpetMod).
     *
     * @return true if player is fake (can skip authentication process), otherwise false
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    boolean easyAuth$canSkipAuth();

    /**
     * Whether the player is using the mojang account
     *
     * @return true if paid, false if cracked
     */
    boolean easyAuth$isUsingMojangAccount();

    /**
     * Gets the player's IP address on connection step.
     *
     * @return player's IP address as string
     */
    String easyAuth$getIpAddress();

    /**
     * Sets the player's IP address on connection step.
     *
     * @param ClientConnection connection
     */
    void easyAuth$setIpAddress(ClientConnection connection);
}
