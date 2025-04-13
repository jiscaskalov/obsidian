package dev.jsco.obsidian;

import com.destroystokyo.paper.util.VersionFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.protocol.status.ServerStatus;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public class ObsidianVersionFetcher implements VersionFetcher {

    private static final Logger LOGGER = Logger.getLogger("obsidian-verfetcher");
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String DOWNLOAD_PAGE = "https://github.com/jiscaskalov/obsidian";

    private static final String GITHUB_FORMAT = "https://api.github.com/repos/jiscaskalov/obsidian/compare/main...%s";
    private static final HttpResponse.BodyHandler<JsonObject> JSON_OBJECT_BODY_HANDLER = responseInfo -> HttpResponse.BodySubscribers
        .mapping(
            HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
            string -> new Gson().fromJson(string, JsonObject.class)
        );

    @Override
    public long getCacheTime() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Override
    public @NotNull Component getVersionMessage(final @NotNull String serverVersion) {
        @NotNull Component msg;
        @NotNull Component versionMessage;
        @NotNull String version;

        final ServerBuildInfo versionInfo = ServerBuildInfo.buildInfo();

        if (versionInfo.gitCommit().isPresent()) {
            msg = this.fetchGithubVersion(versionInfo.gitCommit().get());
            version = versionInfo.gitCommit().get();
        } else {
            msg = this.getResponseMessage(-2);
            version = versionInfo.buildTime().truncatedTo(ChronoUnit.SECONDS).toString();
        }

        versionMessage = Component.text(Bukkit.getName(), GRAY)
            .append(text(" (" + version + " - " + versionInfo.minecraftVersionId() + "+" + ServerStatus.Version.current().protocol() + ")", DARK_GRAY));

        return text("Server version: ")
            .append(versionMessage)
            .hoverEvent(Component.text("Click to copy to clipboard", WHITE))
            .clickEvent(ClickEvent.copyToClipboard(PlainTextComponentSerializer.plainText().serialize(versionMessage)))
            .append(msg);
    }

    // Based off code contributed by Techcable <Techcable@outlook.com> in Paper/GH-65
	private @NotNull Component fetchGithubVersion(final @NotNull String hash) {
        final URI uri = URI.create(String.format(GITHUB_FORMAT, hash));
        final HttpRequest request = HttpRequest.newBuilder(uri).build();
        try {
            final HttpResponse<JsonObject> response = CLIENT.send(request, JSON_OBJECT_BODY_HANDLER);
            if (response.statusCode() != 200) {
                return text("Received invalid status code (" +  response.statusCode() +  ") from server.", RED);
            }
            
            final JsonObject obj = response.body();
            final int versionDiff = obj.get("behind_by").getAsInt();
            
            return this.getResponseMessage(versionDiff);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Failed to look up version from GitHub", e);
            return text("Failed to retrieve version from server.", RED);
        }
    }

    private @NotNull Component getResponseMessage(final int versionDiff) {
        return switch (Math.max(-2, Math.min(1, versionDiff))) {
            case -2 -> Component.newline().append(text("WARNING - Development builds are not supported!", RED));
            case -1 -> Component.newline().append(text("Unknown version!", RED));
            case 0 -> text("");
            default -> Component.newline().append(text("Update to the latest version at ")
                .append(text(DOWNLOAD_PAGE, GRAY))
                    .hoverEvent(text("Click to open", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.openUrl(DOWNLOAD_PAGE))
                    .append(Component.newline())
                    .append(text(versionDiff + " version(s) out of date!", RED)));
        };
    }
}
