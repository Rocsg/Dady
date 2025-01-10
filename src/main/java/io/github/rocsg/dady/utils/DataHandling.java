package io.github.rocsg.dady.utils;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
public class DataHandling {

    public static void main(String[] args) {
        System.out.println(getUserPreprocessingDataPath());
    }

    public static String getUserPreprocessingDataPath(){
        return getPropertyInConfigJson("user_data_preprocessing_path");
    }

    public static String getPropertyInConfigJson(String propertyKey) {
        String repoDir=System.getProperty("user.dir");
        // Chemin par défaut
        String defaultProperty = "Default property";
        String propertyValue = defaultProperty;

        // Fichier de configuration
        String configFile = repoDir+"/data_config.json";
        try {
            Scanner scanner = new Scanner(new FileReader(configFile));
            StringBuilder jsonContent = new StringBuilder();
            while (scanner.hasNextLine()) {
                jsonContent.append(scanner.nextLine());
            }
            scanner.close();

            // Convertir JSON en Map (parsing rudimentaire)
            String json = jsonContent.toString().replaceAll("[{}\"]", "");
            Map<String, String> config = new HashMap<>();
            for (String entry : json.split(",")) {
                String[] keyValue = entry.split(":");
                if (keyValue.length == 2) {
                    config.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }

            // Récupérer le chemin utilisateur
            propertyValue = config.getOrDefault(propertyKey, defaultProperty);

        } catch (Exception e) {
            System.out.println("Fichier non trouvé ou erreur de lecture, chemin par défaut utilisé.");
        }

        System.out.println("Property read : "+propertyValue);
        return propertyValue;
    }
}