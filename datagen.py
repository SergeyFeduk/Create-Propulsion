# I'm fed up with gradle bullshit. I just need to generate envelopes and I have lessons tomorrow

import os
import json

def generate_block_files(base_name, colors):
    base_path = os.path.join("src", "generated", "resources")
    
    directories = [
        os.path.join(base_path, "assets", "createpropulsion", "blockstates"),
        os.path.join(base_path, "assets", "createpropulsion", "models", "block"),
        os.path.join(base_path, "assets", "createpropulsion", "models", "item")
    ]
    
    for directory in directories:
        os.makedirs(directory, exist_ok=True)
    
    for color in colors:
        if color == "white":
            filename = f"{base_name}.json"
            model_path = f"createpropulsion:block/{base_name}/{base_name}"
            bs_path = f"createpropulsion:block/{base_name}"
        else:
            filename = f"{base_name}_{color}.json"
            model_path = f"createpropulsion:block/{base_name}/{base_name}_{color}"
            bs_path = f"createpropulsion:block/{base_name}_{color}"
        
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

def generate_recipes(colors):
    base_path = os.path.join("src", "generated", "resources")
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

def generate_files():
    colors = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
    base_names = ["envelope", "enveloped_shaft"]
    
    for base_name in base_names:
        generate_block_files(base_name, colors)
    
    generate_recipes(colors)

if __name__ == "__main__":
    generate_files()