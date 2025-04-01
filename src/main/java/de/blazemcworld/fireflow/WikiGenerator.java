package de.blazemcworld.fireflow;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.node.NodeList;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WikiGenerator {

    private static StringBuilder sidebar;
    private static int sidebarDepth = 0;

    public static void main(String[] args) {
        NodeList.init();

        sidebar = new StringBuilder();
        Translations.use("en_US");
        sidebar("Nodes", "nodes");
        sidebarDepth++;
        generateCategory(NodeList.root, "nodes");
        sidebarDepth--;

        try {
            Path p = Path.of("wiki/_sidebar.md");
            if (!Files.exists(p.getParent())) Files.createDirectories(p.getParent());
            Files.writeString(p, sidebar.toString(), StandardOpenOption.APPEND);
            System.out.println("Generated sidebar.md");
        } catch (IOException e) {
            System.err.println("Failed to write sidebar.md!");
            e.printStackTrace();
        }
    }

    public static void sidebar(String name, String path) {
        sidebar.append("  ".repeat(sidebarDepth)).append("* [").append(name).append("](/").append(path).append(".md)\n");
    }

    private static void generateCategory(NodeList.Category category, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ");
        if (category.icon != null) sb.append(" ").append(icon(category.icon));
        sb.append(Translations.get(category.name));
        sb.append("\n\n");

        if (!category.categories.isEmpty()) {
            sb.append("## Categories\n");
            for (NodeList.Category c : category.categories) {
                if (c.isFunctions) continue;
                String newPath = path + "/" + Translations.get(c.name).toLowerCase().replace(' ', '_');
                sidebar(Translations.get(c.name), newPath);
                sidebarDepth++;
                generateCategory(c, newPath);
                sidebarDepth--;
                sb.append("- ");
                if (c.icon != null) sb.append(icon(c.icon));
                sb.append("[").append(Translations.get(c.name)).append("](/").append(path).append("/").append(Translations.get(c.name).toLowerCase().replace(' ', '_')).append(".md)\n");
            }
            sb.append("\n");
        }

        if (!category.nodes.isEmpty()) {
            sb.append("## Nodes\n");
            for (Node node : category.nodes) {
                generateNode(node, path + "/" + node.getTitle().toLowerCase().replace(' ', '_'));
                sb.append("- ").append(icon(node.icon)).append("[").append(node.getTitle()).append("](/").append(path).append("/").append(node.getTitle().toLowerCase().replace(' ', '_')).append(".md)\n");
            }
        }

        try {
            Path p = Path.of("wiki/" + path + ".md");
            if (!Files.exists(p.getParent())) Files.createDirectories(p.getParent());
            Files.writeString(p, sb.toString());
            System.out.println("Generated " + path + ".md");
        } catch (IOException e) {
            System.err.println("Failed to write " + path + ".md!");
            e.printStackTrace();
        }
    }

    private static void generateNode(Node node, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(icon(node.icon)).append(node.getTitle()).append("\n\n");
        sb.append(node.getWikiDescription()).append("\n\n");
        if (!node.inputs.isEmpty()) {
            sb.append("**Inputs**\n");
            for (Node.Input<?> input : node.inputs) {
                sb.append("- ");
                if (input.type != null) sb.append(colorStart(input.type.color));
                sb.append(input.getName());
                if (input.type != null) sb.append(": ").append(icon(input.type.icon)).append(input.type.getName());
                if (input.options != null) {
                    sb.append("  \nOne of:\n");
                    for (String option : input.options) {
                        sb.append("  - ").append(option).append("\n");
                    }
                }
                if (input.varargsParent != null) sb.append(" (Varargs)");
                if (input.type != null) sb.append(colorEnd());
                sb.append("\n");
            }
            sb.append("\n");
        }
        if (!node.outputs.isEmpty()) {
            sb.append("**Outputs**\n");
            for (Node.Output<?> output : node.outputs) {
                sb.append("- ");
                if (output.type != null) sb.append(colorStart(output.type.color));
                sb.append(output.getName());
                if (output.type != null) {
                    sb.append(" (").append(icon(output.type.icon)).append(output.type.getName()).append(")").append(colorEnd());
                }
                sb.append("\n");
            }
        }
        try {
            Path p = Path.of("wiki/" + path + ".md");
            if (!Files.exists(p.getParent())) Files.createDirectories(p.getParent());
            Files.writeString(p, sb.toString());
            System.out.println("Generated " + path + ".md");
        } catch (IOException e) {
            System.err.println("Failed to write " + path + ".md!");
            e.printStackTrace();
        }
    }

    private static String icon(Material m) {
        return "<img src=\"https://raw.githubusercontent.com/Owen1212055/mc-assets/refs/heads/main/assets/" + m.key().value().toUpperCase() + ".png\" style=\"height: 1em; transform: translateY(0.1em);\">";
    }

    private static String colorStart(TextColor color) {
        return "<span style=\"color: " + color.asHexString() + " !important\">";
    }

    private static String colorEnd() {
        return "</span>";
    }

}
