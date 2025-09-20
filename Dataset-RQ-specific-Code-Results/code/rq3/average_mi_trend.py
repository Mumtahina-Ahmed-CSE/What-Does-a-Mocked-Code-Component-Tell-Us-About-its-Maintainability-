import os
import pandas as pd
import numpy as np
import glob
import matplotlib.pyplot as plt
from util import graphs  # Your plotting functions using matplotlib

# Define folders for each method type
folders = {
    "Mocked": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/MockedMethodMocked/",
    "Unmocked": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/UnmockedMethod/",
    "Impacted": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/ImpactedByMocking/"
}

OUTPUT_DIR = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/results/RQ3"
os.makedirs(OUTPUT_DIR, exist_ok=True)  # Ensure output folder exists

bin_width = 500
max_age = 6500

def parse_series(series):
    try:
        return [float(x) for x in str(series).split("#") if x.strip()]
    except:
        return []

grouped_mi = {"Mocked": {}, "Unmocked": {}, "Impacted": {}}
total_rows = {"Mocked": 0, "Unmocked": 0, "Impacted": 0}
valid_points = {"Mocked": 0, "Unmocked": 0, "Impacted": 0}

for group, folder in folders.items():
    all_files = glob.glob(os.path.join(folder, "*.csv"))
    print(f"📂 Processing {group} folder: {len(all_files)} files")
    
    for file in all_files:
        try:
            df = pd.read_csv(file, sep='\t')
        except Exception as e:
            print(f"⚠️ Error reading {file}: {e}")
            continue
        
        for _, row in df.iterrows():
            total_rows[group] += 1

            #selected_features = ["Readability", "Difficulty", "Effort", "MaintainabilityIndex", "NVAR"]

            ages = parse_series(row.get("ChangeAtMethodAge", ""))
            mis = parse_series(row.get("NVAR", ""))

            if len(ages) != len(mis) or len(ages) == 0:
                continue

            for age, mi in zip(ages, mis):
                if age > max_age:
                    continue
                bin_id = int(age // bin_width)
                grouped_mi[group].setdefault(bin_id, []).append(mi)
                valid_points[group] += 1

# Summary
print("\n✅ Data Summary:")
for group in folders.keys():
    print(f"{group}: {total_rows[group]} rows → {valid_points[group]} valid MI points")

# Compute averages
avg_mi_per_group = {}
for group, bins in grouped_mi.items():
    if not bins:
        print(f"⚠️ No data points found for {group}")
        continue
    ages = sorted(bins.keys())
    avg_mi = [np.mean(bins[age]) for age in ages]
    avg_mi_per_group[group] = (np.array(ages) * bin_width, avg_mi)

# Prepare data for plot
X = []
Y = []
labels = []

for group, (ages, mi) in avg_mi_per_group.items():
    if len(ages) > 0:
        X.append(ages)
        Y.append(mi)
        labels.append(group)

if X and Y:
    output_path = os.path.join(OUTPUT_DIR, "MaximumBlockDepth-age.png")
    config = {
        "x_label": "Method Age (Days)",
        "y_label": "MaximumBlockDepth",
        "legends": labels,
        "marker": True,
        "output_path": output_path  # Pass here so plot saves automatically
    }

    graphs.draw_line_graph_multiple_with_x(X, Y, config)

    print(f"✅ Plot saved to: {output_path}")
else:
    print("⚠️ No valid data to plot. Check if MI and age values exist in the CSVs.")
