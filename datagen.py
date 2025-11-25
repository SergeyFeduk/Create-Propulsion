# I'm fed up with gradle bullshit. I just need to generate envelopes and I have lessons tomorrow

import os
import json

def generate_files():
    base_path = os.path.join("src", "generated", "resources")
    colors = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
    
    directories = [
        os.path.join(base_path, "assets", "createpropulsion", "blockstates"),
        os.path.join(base_path, "assets", "createpropulsion", "models", "block"),
        os.path.join(base_path, "assets", "createpropulsion", "models", "item")
    ]
    
    for directory in directories:
        os.makedirs(directory, exist_ok=True)
    
    for color in colors:
        if color == "white":
            filename = "envelope.json"
            model_path = "createpropulsion:block/envelope/envelope"
            bs_path = "createpropulsion:block/envelope"
        else:
            filename = f"envelope_{color}.json"
            model_path = f"createpropulsion:block/envelope/envelope_{color}"
            bs_path = f"createpropulsion:block/envelope_{color}"
        
        blockstate_content = {
            "variants": {
                "": {
                    "model": bs_path
                }
            }
        }
        
        model_content = {
            "parent": "block/cube_all",
            "textures": {
                "all": model_path
            }
        }
        
        blockstate_path = os.path.join(directories[0], filename)
        block_model_path = os.path.join(directories[1], filename)
        item_model_path = os.path.join(directories[2], filename)
        
        with open(blockstate_path, 'w') as f:
            json.dump(blockstate_content, f, indent=2)
        
        with open(block_model_path, 'w') as f:
            json.dump(model_content, f, indent=2)
        
        with open(item_model_path, 'w') as f:
            json.dump(model_content, f, indent=2)
    
    recipe_dir = os.path.join(base_path, "data", "createpropulsion", "recipes", "crafting")
    os.makedirs(recipe_dir, exist_ok=True)
    
    for color in colors:
        if color == "white":
            continue
        
        recipe_filename = f"envelope_{color}.json"
        output_item = f"createpropulsion:envelope_{color}"
        
        recipe_content = {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [
                {
                    "item": "createpropulsion:envelope"
                },
                {
                    "item": f"minecraft:{color}_dye"
                }
            ],
            "result": {
                "item": output_item,
                "count": 1
            }
        }
        
        recipe_path = os.path.join(recipe_dir, recipe_filename)
        with open(recipe_path, 'w') as f:
            json.dump(recipe_content, f, indent=2)

if __name__ == "__main__":
    generate_files()