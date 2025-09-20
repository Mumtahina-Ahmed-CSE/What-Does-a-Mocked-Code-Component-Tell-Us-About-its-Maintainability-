import os
import pandas as pd
import numpy as np
import glob
from scipy.stats import kruskal, mannwhitneyu

# Folders for each group
folders = {
    "Mocked": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/MockedMethodMocked/",
    "Unmocked": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/UnmockedMethod/",
    "Impacted": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/ImpactedByMocking/"
}

max_age = 6500  # max method age cutoff

def parse_series(series):
    try:
        return [float(x) for x in str(series).split("#") if x.strip()]
    except:
        return []

# Collect all maintainability index points for each group (all ages <= max_age)
all_mi_points = {"Mocked": [], "Unmocked": [], "Impacted": []}

for group, folder in folders.items():
    all_files = glob.glob(os.path.join(folder, "*.csv"))
    print(f"Processing {group}: {len(all_files)} files")
    
    for file in all_files:
        try:
            df = pd.read_csv(file, sep='\t')
        except Exception as e:
            print(f"Error reading {file}: {e}")
            continue
        
        for _, row in df.iterrows():
            ages = parse_series(row.get("ChangeAtMethodAge", ""))
            mis = parse_series(row.get("Parameters", "")) #selected_features = ["Readability", "Difficulty", "Effort", "MaintainabilityIndex", "NVAR"]

            if len(ages) != len(mis) or len(ages) == 0:
                continue
            
            for age, mi in zip(ages, mis):
                if age <= max_age:
                    all_mi_points[group].append(mi)

# Summary of points collected
print("\n--- MI Points Collected ---")
group_sizes = {}
for group, points in all_mi_points.items():
    group_sizes[group] = len(points)
    print(f"{group}: {len(points)} MI points")

# Kruskal-Wallis test
print("\n--- Kruskal-Wallis Test ---")
kw_stat, kw_p = kruskal(all_mi_points["Mocked"], all_mi_points["Unmocked"], all_mi_points["Impacted"])
print(f"H-statistic: {kw_stat:.4f}, p-value: {kw_p:.4e}")

# Mann-Whitney U and formatted table if significant
if kw_p < 0.05:
    print("\nSignificant differences detected! Running pairwise Mann-Whitney U tests:\n")
    pairs = [("Impacted", "Unmocked"), ("Impacted", "Mocked"), ("Mocked", "Unmocked")]
    results = []

    for g1, g2 in pairs:
        data1 = all_mi_points[g1]
        data2 = all_mi_points[g2]
        n1, n2 = len(data1), len(data2)

        u_stat, u_p = mannwhitneyu(data1, data2, alternative='two-sided')
        r_rb = 1 - (2 * u_stat) / (n1 * n2)

        # Fix sign based on median
        median1 = np.median(data1)
        median2 = np.median(data2)
        if median1 < median2:
            r_rb = -abs(r_rb)
        else:
            r_rb = abs(r_rb)

        # Determine effect size interpretation
        abs_r = abs(r_rb)
        if abs_r < 0.1:
            interpretation = "negligible"
        elif abs_r < 0.3:
            interpretation = "small"
        elif abs_r < 0.5:
            interpretation = "medium"
        else:
            interpretation = "large"

        sign = "+" if r_rb > 0 else "-" if r_rb < 0 else "0"

        results.append({
            "Comparison": f"{g1} vs {g2}",
            "P": f"{u_p:.2f}",
            "Sign": sign,
            "Effect": interpretation
        })

    # Display LaTeX-style table rows
    print("--- LaTeX Table Rows ---")
    for row in results:
        print(f"{row['Comparison']} & {row['P']} & {row['Sign']} & {row['Effect']}")

else:
    print("\nNo significant differences found by Kruskal-Wallis test.")
