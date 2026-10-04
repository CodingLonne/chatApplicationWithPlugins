import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Launcher {
    public static List<ClientPlugin> clientPlugins;
    public static List<ServerPlugin> serverPlugins;
    public static void main(String[] args) throws IOException, InterruptedException {
        // java Launcher client host port name [color]
        // java Launcher server [port] [log-file]
        clientPlugins = List.of(new ClientAuthPlugin()/*color, encryptions, auth, logging */);
        serverPlugins = List.of(new ServerAuthPlugin());
        
        if (args.length == 0){
            System.out.println("Usage: java Launcher.java [client/server]");
            return;
        }
        if (args[0].equals("client")) {
            if (args.length < 3){
                System.out.println("Usage: java Launcher.java client localhost port username [color]");
                return;
            }
            String host = args[1];
            var port = Integer.parseInt(args[2]);
            String userName = args[3];
            // plugin arguments
            passClientPluginArgs(host, port, userName, args);
            ChatClient.start_client(host, port, userName);
        } else if (args[0].equals("server")) {
            //server
            if (args.length > 3) {
                System.out.println("Usage: java Launcher.java server [port] [log-file]");
                return;
            }
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
            MessageLog log = new MessageLog(args.length > 1 ? args[2] : "server.log");
            try {
                new ChatServer(log).serve(port);
            } finally {
                log.close();
            }
        }
    }

    private static void passClientPluginArgs(String host, int port, String name, String[] args) {
        divideClientPluginArgs(
            Arrays.copyOfRange(args, 4, args.length), 
            p -> p.getArgCount(), 
            (p, args2) -> p.setNeededArgs(host, port, name, args2));
    }

    public static Map<ClientPlugin, String[]> divideClientPluginArgs(String[] args, Function<ServerPlugin, Integer> pluginToArgCount) {
        Map<ClientPlugin, String[]> correspondanceMap = new HashMap<>(Launcher.clientPlugins.size());
        int index = 3;
        for (ClientPlugin plugin : Launcher.clientPlugins) {
            int num_args_plugin = plugin.getArgCount();
            correspondanceMap.put(plugin, Arrays.copyOfRange(args, index, index+num_args_plugin));
            index += num_args_plugin;
        }
        return correspondanceMap;
    }

    public static void divideClientPluginArgs(String[] args, Function<ServerPlugin, Integer> pluginToArgCount, BiConsumer<ClientPlugin, String[]> doingFunction) {
        int index = 3;
        for (ClientPlugin plugin : Launcher.clientPlugins) {
            int num_args_plugin = plugin.getArgCount();
            doingFunction.accept(plugin, Arrays.copyOfRange(args, index, index+num_args_plugin));
            index += num_args_plugin;
        }
    }

    public static Map<ServerPlugin, String[]> divideServerPluginArgs(String[] args, Function<ServerPlugin, Integer> pluginToArgCount) {
        Map<ServerPlugin, String[]> correspondanceMap = new HashMap<>(Launcher.serverPlugins.size());
        int index = 3;
        for (ServerPlugin plugin : Launcher.serverPlugins) {
            int num_args_plugin = plugin.getArgCount();
            correspondanceMap.put(plugin, Arrays.copyOfRange(args, index, index+num_args_plugin));
            index += num_args_plugin;
        }
        return correspondanceMap;
    }

    public static void divideServerPluginArgs(String[] args, Function<ServerPlugin, Integer> pluginToArgCount, BiConsumer<ServerPlugin, String[]> doingFunction) {
        int index = 3;
        for (ServerPlugin plugin : Launcher.serverPlugins) {
            int num_args_plugin = plugin.getArgCount();
            doingFunction.accept(plugin, Arrays.copyOfRange(args, index, index+num_args_plugin));
            index += num_args_plugin;
        }
    }
}
