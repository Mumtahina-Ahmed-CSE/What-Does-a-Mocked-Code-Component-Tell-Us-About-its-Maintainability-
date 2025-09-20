import os
import pandas as pd
import numpy as np

folders = [
    "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/MockedMethodMocked/",
    "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/UnmockedMethod/",
    "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/ImpactedByMocking/"
]

all_ages = []

for folder in folders:
    for filename in os.listdir(folder):
        if filename.endswith(".csv"):
            filepath = os.path.join(folder, filename)
            try:
                df = pd.read_csv(filepath, sep='\t', usecols=["Age"])
                ages = df["Age"].dropna().astype(int).tolist()
                all_ages.extend(ages)
            except Exception as e:
                print(f"Error reading {filepath}: {e}")

# Compute and print custom thresholds
if all_ages:
    max_age_actual = max(all_ages)
    mean_age = np.mean(all_ages)
    median_age = np.median(all_ages)
    percentile_95 = np.percentile(all_ages, 95)

    print(f"📊 Method Age Stats:")
    print(f"   Max     = {max_age_actual}")
    print(f"   Mean    = {mean_age:.2f}")
    print(f"   Median  = {median_age}")
    print(f"   95th %  = {percentile_95:.0f}")

    # Set max_age based on 95th percentile
    max_age = percentile_95
else:
    print("⚠️ No method ages found.")
    max_age = 2500  # fallback
