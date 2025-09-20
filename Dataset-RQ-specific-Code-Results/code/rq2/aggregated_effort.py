import os
from util import utility
from util import graphs
#from util import graphss
#from util import graph
import cliffsDeltaModule
from scipy import stats

# Input data paths
SRC_PATHS = {
    "Mocked": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/MockedMethodMocked/",
    "Unmocked": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/UnmockedMethod/",
    "Impacted": "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned/ImpactedByMocking/"
}

#selected_features = ["Effort", "Time", "Volume", "HalsteadBugs", "Difficulty"]
selected_features = ["Difficulty"]
                     

# Output directory
OUTPUT_DIR =  "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/results/RQ2"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def find_index(sample_path):
    indexes = {}
    with open(os.path.join(sample_path, "checkstyle.csv")) as fr:
        line = fr.readline().strip()
        data = line.split("\t")
        for i in range(len(data)):
            indexes[data[i]] = i
    return indexes

def getMean(ls):
    return sum([float(v) for v in ls]) / len(ls)

def process(feature, indexes, src_path):
    values = []
    for file in os.listdir(src_path):
        with open(os.path.join(src_path, file), encoding="utf-8", errors="replace") as fr:
            fr.readline()  # Skip header
            for line in fr:
                line = line.strip()
                data = line.split("\t")
                feature_values = data[indexes[feature]].split("#")
                value = float(feature_values[-1])  # Use latest value
                values.append(value)
    return values

def statistics(x, y):
    d, size = cliffsDeltaModule.cliffsDelta(x, y)
    st = stats.mannwhitneyu(x, y)
    p_value = st[1]  
    return p_value, d, size

def formatLatex(feature, p, d, size):
    display_feature = 'Size' if feature == 'SLOCStandard' else feature
    direction = "-" if float(d) < 0 else "+"
    return f"{display_feature}&{p:.2f}&{direction}&{size}\\\\\n"

def draw_graph(feature, method_data):
    X = []
    Y = []
    legends = []

    for method, values in method_data.items():
        x, y = utility.ecdf(values)
        X.append(x)
        Y.append(y)
        legends.append(method)

    configs = {
        "x_label": "Size" if feature == "SLOCStandard" else feature,
        "y_label": "CDF",
        "legends": legends,
        #"xlim": (0, 1),      # 👈 for HalsteadBugs
        "xlim": (1, None),
        "xscale": True,
        #"x_ticks": [0.2, 0.4, 0.6, 0.8, 1.0],
        #"xlim": (60, None),  # 👈 ensures X-axis starts from 60
        "output_path": os.path.join(OUTPUT_DIR, f"{feature}.png")  # <-- Changed to .png
    }

    #graphss.draw_line_graph_multiple_with_x(X, Y, configs)
    graphs.draw_line_graph_multiple_with_x(X, Y, configs)
    #graph.draw_line_graph_multiple_with_x(X, Y, configs)

if __name__ == "__main__":
    sample_path = list(SRC_PATHS.values())[0]  # Use any to get column indexes
    indexes = find_index(sample_path)

    output_file_path = os.path.join(OUTPUT_DIR, "results.txt")
    with open(output_file_path, "w") as out:
        for feature in selected_features:
            method_data = {}
            for method_type, path in SRC_PATHS.items():
                method_data[method_type] = process(feature, indexes, path)

            # Draw CDF
            draw_graph(feature, method_data)

            # Statistical comparisons:
            comparisons = [
                ("Impacted", "Unmocked"),
                ("Impacted", "Mocked"),
                ("Mocked", "Unmocked")
                
            ]

            for m1, m2 in comparisons:
                p, d, size = statistics(method_data[m1], method_data[m2])
                line = f"{m1} vs {m2} - " + formatLatex(feature, p, d, size)
                out.write(line)
                print(line.strip())

    print(f"\nResults saved to: {output_file_path}")
    print(f"All graphs saved as PDFs in: {OUTPUT_DIR}")