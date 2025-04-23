package org.plugin.bantools;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public class LoginListener {
    private final BanManager banManager;

    public LoginListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (banManager.isBanned(
                player.getUniqueId().toString(),
                player.getRemoteAddress().getAddress().getHostAddress(),
                player.getUsername()
        )) {
            event.setResult(ResultedEvent.ComponentResult.denied(
                    Component.text(banManager.getBanMessage(
                            player.getUniqueId().toString(),
                            player.getRemoteAddress().getAddress().getHostAddress(),
                            player.getUsername()
                    ))
            ));
        }
    }
}