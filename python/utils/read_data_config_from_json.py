import json
import os

def load_json_config(config_filename="data_config.json", default_config=None):
    """
    Charge un fichier JSON de configuration.

    :param config_filename: Nom du fichier de configuration JSON (par défaut : "data_config.json").
    :param default_config: Dictionnaire des valeurs par défaut (facultatif).
    :return: Dictionnaire contenant la configuration.
    """
    if default_config is None:
        default_config = {}

    # Identifier la racine du projet (Dady)
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../"))
    config_path = os.path.join(project_root, config_filename)

    try:
        with open(config_path, "r") as file:
            config = json.load(file)
    except FileNotFoundError:
        print(f"Fichier de configuration non trouvé : {config_path}")
        config = default_config
    except json.JSONDecodeError:
        print(f"Erreur dans le fichier JSON : {config_path}")
        config = default_config

    return config


def get_user_data_path(config_filename="data_config.json", default_path="/chemin/par/defaut"):
    """
    Récupère le chemin des données utilisateur à partir d'un fichier JSON.

    :param config_filename: Nom du fichier de configuration JSON (par défaut : "data_config.json").
    :param default_path: Chemin par défaut si le fichier ou la clé est absent(e).
    :return: Chemin des données utilisateur (str).
    """
    # Charger la configuration
    config = load_json_config(config_filename=config_filename, default_config={"user_data_path": default_path})
    print(config)
    # Récupérer et retourner le chemin
    return config.get("user_data_preprocessing_path", default_path)


# Exemple d'utilisation (pour tester le fichier directement)
if __name__ == "__main__":
    user_data_path = get_user_data_path()
    print(f"Chemin local des données : {user_data_path}")
