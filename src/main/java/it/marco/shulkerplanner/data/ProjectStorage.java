package it.marco.shulkerplanner.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;

import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import it.marco.shulkerplanner.Reference;
import it.marco.shulkerplanner.ShulkerPlanner;

/**
 * Loads and saves the per-schematic projects as json files in config/shulkerplanner/projects/
 */
public class ProjectStorage
{
    public static Path getBaseDirectory()
    {
        return FileUtils.getConfigDirectory().resolve(Reference.MOD_ID);
    }

    public static Path getProjectDirectory()
    {
        return getBaseDirectory().resolve("projects");
    }

    public static Path getExportDirectory()
    {
        return getBaseDirectory().resolve("exports");
    }

    public static String getSafeFileName(String name)
    {
        StringBuilder sb = new StringBuilder();

        for (char c : name.toCharArray())
        {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.')
            {
                sb.append(c);
            }
            else
            {
                sb.append('_');
            }
        }

        String safe = sb.toString();

        if (safe.isEmpty() || safe.chars().allMatch(c -> c == '_' || c == '.'))
        {
            safe = "unnamed";
        }

        if (safe.length() > 80)
        {
            safe = safe.substring(0, 80);
        }

        // Avoid collisions between names that only differ in the replaced characters
        if (safe.equals(name) == false)
        {
            safe += "_" + Integer.toHexString(name.hashCode());
        }

        return safe;
    }

    private static Path getProjectFile(String name)
    {
        return getProjectDirectory().resolve(getSafeFileName(name) + ".json");
    }

    public static boolean exists(String name)
    {
        return Files.isRegularFile(getProjectFile(name));
    }

    public static PlannerProject load(String name)
    {
        Path file = getProjectFile(name);

        if (Files.isRegularFile(file))
        {
            try
            {
                JsonElement el = JsonUtils.parseJsonFile(file);

                if (el != null && el.isJsonObject())
                {
                    return PlannerProject.fromJson(el.getAsJsonObject(), name);
                }
            }
            catch (Exception e)
            {
                ShulkerPlanner.LOGGER.error("Failed to read the project file '{}'", file.toAbsolutePath(), e);
            }
        }

        return new PlannerProject(name);
    }

    public static void save(PlannerProject project)
    {
        Path dir = getProjectDirectory();

        if (FileUtils.createDirectoriesIfMissing(dir) == false && Files.isDirectory(dir) == false)
        {
            ShulkerPlanner.LOGGER.error("Could not create the directory '{}'", dir.toAbsolutePath());
            return;
        }

        JsonUtils.writeJsonToFile(project.toJson(), getProjectFile(project.getName()));
    }

    /**
     * Writes the export of a project. Every project has a single export file which gets overwritten,
     * so exporting again never fills the folder with copies.
     */
    @Nullable
    public static Path writeExport(String projectName, String text)
    {
        Path dir = getExportDirectory();
        FileUtils.createDirectoriesIfMissing(dir);
        Path file = dir.resolve(getSafeFileName(projectName) + ".txt");

        try
        {
            Files.writeString(file, text, StandardCharsets.UTF_8);
            return file;
        }
        catch (IOException e)
        {
            ShulkerPlanner.LOGGER.error("Failed to write the export file '{}'", file.toAbsolutePath(), e);
            return null;
        }
    }
}
