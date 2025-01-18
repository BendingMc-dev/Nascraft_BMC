package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import org.bukkit.configuration.file.FileConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImagesManager {

    private static ImagesManager instance;

    public static ImagesManager getInstance() { return instance == null ? instance = new ImagesManager() : instance; }


    public BufferedImage getImage(String identifier) {

        FileConfiguration items = Config.getInstance().getItemsFileConfiguration();

        BufferedImage image = null;
        String imageName;
        String imagePath = Nascraft.getInstance().getDataFolder().getPath() + "/images/" + identifier + ".png";

        try (InputStream input = Files.newInputStream(new File(imagePath).toPath())) {

            image = ImageIO.read(input);

        } catch (IOException ignored) {
            // No image specified.
        } catch (IllegalArgumentException e) {
            Nascraft.getInstance().getLogger().info("Invalid argument for image: " + identifier);
        }

        if (image != null) return image;

        if (items.contains("items." + identifier + ".item-stack.type")) {

            imageName = items.getString("items." + identifier + ".item-stack.type").toLowerCase() + ".png";
            imagePath = "1-20-materials/minecraft_" + imageName;

            try (InputStream input = Nascraft.getInstance().getResource(imagePath)) {
                if (input != null) {
                    image = ImageIO.read(input);
                } else {
                    Nascraft.getInstance().getLogger().info("Unable to find image: " + imageName);
                }
            } catch (IOException e) {
                Nascraft.getInstance().getLogger().info("Unable to read image: " + imageName);
            } catch (IllegalArgumentException e) {
                Nascraft.getInstance().getLogger().info("Invalid argument for image: " + imageName);
            }

            return image;
        }

        imageName = identifier.replaceAll("\\d", "").replaceFirst("_t$", "").toLowerCase() + ".png";

        try (InputStream input = Nascraft.getInstance().getResource("1-20-materials/minecraft_" + imageName)) {
            if (input != null) {
                image = ImageIO.read(input);
            } else {
                Nascraft.getInstance().getLogger().info("Unable to find image: " + imageName);
            }
        } catch (IOException e) {
            Nascraft.getInstance().getLogger().info("Unable to read image: " + imageName);
        } catch (IllegalArgumentException e) {
            Nascraft.getInstance().getLogger().info("Invalid argument for image: " + imageName);
        }

        if (image != null) return image;

        try {
            Path itemsAdderPath = Paths.get("plugins/ItemsAdder/contents");

            File foundFile = Files.walk(itemsAdderPath)
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().equalsIgnoreCase(imageName))
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);

            if (foundFile != null) {
                Nascraft.getInstance().getLogger().info("Found image: " + foundFile.getName());
                Files.copy(foundFile.toPath(), new File(imagePath).toPath());
                try (InputStream input = Files.newInputStream(new File(imagePath).toPath())) {
                    image = ImageIO.read(input);
                    return image;
                }
            } else {
                Nascraft.getInstance().getLogger().info("Image file not found in IA Dir: " + imageName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    public static byte[] getBytesOfImage(BufferedImage image) {
        ByteArrayOutputStream baosBalance = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baosBalance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baosBalance.toByteArray();
    }

}
