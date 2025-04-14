package org.plugin.bantools;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import java.util.Optional;

public class LoginListener {

    private final BanManager banManager;

    public LoginListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLogin(LoginEvent event) {
        Optional<Player> playerOptional = Optional.ofNullable(event.getPlayer());
        if (playerOptional.isEmpty()) return;

        Player player = playerOptional.get();
        String username = player.getUsername();
        String uuid = player.getUniqueId().toString();
        String ipAddress = player.getRemoteAddress().getAddress().getHostAddress();

        // 按权重检查封禁条件
        if (banManager.isBanned(uuid, ipAddress, username)) {
            event.setResult(ResultedEvent.ComponentResult.denied(
                    Component.text(banManager.getBanMessage(uuid, ipAddress, username))
            ));
        }
    }
}