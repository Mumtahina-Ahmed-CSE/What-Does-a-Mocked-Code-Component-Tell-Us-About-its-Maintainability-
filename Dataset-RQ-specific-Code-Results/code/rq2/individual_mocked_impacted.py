import os
from util import utility
from util import graphs
import cliffsDeltaModule
from scipy import stats

# Updated paths for Mocked and Impacted methods
MOCKED_PATH = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/MockedMethodMocked/"
IMPACTED_PATH = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/ImpactedByMocking/"

selected_features = ["Effort", "Time", "Volume", "HalsteadBugs", "Difficulty"]

def find_index(sample_file):
    indexes = {}
    with open(sample_file, "r", encoding="utf-8", errors="replace") as fr:
        line = fr.readline().strip()
        headers = line.split("\t")
        for i, col in enumerate(headers):
            indexes[col] = i
    return indexes

def extract_values(path, feature, index):
    data_by_file = {}
    for file in os.listdir(path):
        full_path = os.path.join(path, file)
        if not os.path.isfile(full_path):
            continue
        values = []
        with open(full_path, "r", encoding="utf-8", errors="replace") as fr:
            fr.readline()  # skip header
            for line in fr:
                data = line.strip().split("\t")
                if feature not in index:
                    continue
                val_str = data[index[feature]]
                if val_str.strip() == "":
                    continue
                feature_values = val_str.split("#")
                try:
                    value = float(feature_values[-1])
                    values.append(value)
                except ValueError:
                    continue
        if values:
            data_by_file[file] = values
    return data_by_file

def statistics(x, y):
    d, size = cliffsDeltaModule.cliffsDelta(x, y)
    st = stats.mannwhitneyu(x, y, alternative='two-sided')
    p_value = st[1]
    return p_value, d, size

def calculate_per_project(feature, indexes):
    effect_counts = {'negligible': 0, 'small': 0, 'medium': 0, 'large': 0}
    insignificant = 0
    total_projects = 0

    mocked_data = extract_values(MOCKED_PATH, feature, indexes)
    impacted_data = extract_values(IMPACTED_PATH, feature, indexes)

    common_files = set(mocked_data.keys()) & set(impacted_data.keys())

    for file in common_files:
        x = mocked_data[file]
        y = impacted_data[file]
        if not x or not y:
            continue
        total_projects += 1
        p, d, size = statistics(x, y)
        if p > 0.05:
            insignificant += 1
        else:
            effect_counts[size] += 1

    return total_projects, insignificant, effect_counts

def printLatexProjectLevel(feature, total, insignificant, effects):
    significant = total - insignificant

    # Normalize effect sizes only among significant cases
    if significant == 0:
        norm_effects = {k: 0 for k in effects}
    else:
        norm_effects = {k: 100 * (v / significant) for k, v in effects.items()}

    if feature == 'SLOCStandard':
        feature = 'Size'

    print("{} & {:.2f} & {:.2f} & {:.2f} & {:.2f} & {:.2f} \\\\".format(
        feature,
        100 * (insignificant / total) if total > 0 else 0,
        norm_effects['negligible'],
        norm_effects['small'],
        norm_effects['medium'],
        norm_effects['large']
    ))

if __name__ == "__main__":
    sample_file = os.path.join(MOCKED_PATH, os.listdir(MOCKED_PATH)[0])
    indexes = find_index(sample_file)

    print("Feature & Insignificant(%) & Negligible(%) & Small(%) & Medium(%) & Large(%) \\\\")
    print("\\hline")

    for feature in selected_features:
        total, insignificant, effects = calculate_per_project(feature, indexes)
        printLatexProjectLevel(feature, total, insignificant, effects)
