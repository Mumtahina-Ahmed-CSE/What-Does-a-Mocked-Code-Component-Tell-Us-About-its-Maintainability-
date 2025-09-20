import matplotlib.pyplot as plt

fig, ax = plt.subplots()

styles = ["-", "--", "-.", ":", "--", "--", "-.", ":"]
marks = ["^", "d", "o", "v", "p", "s", "<", ">"]
width = [7, 7, 7, 7, 6, 6, 6, 6]  
marks_size = [20, 15, 12, 14, 20, 10, 12, 15]
marker_color = ['#0F52BA', '#ff7518', '#6CA939', '#e34234', '#756bb1', 'brown', '#c994c7', '#636363']
gaps =  [5, 3, 2, 3, 5, 4, 4, 4]

def draw_line_graph_multiple(lists, config):
    index = 0
    for lst in lists:
        ln = plt.plot(range(1, len(lst) + 1), lst)
        plt.setp(ln, linewidth=width[index], ls=styles[index], color=marker_color[index])
        index += 1

    if "x_label" in config:
        plt.xlabel(config["x_label"], fontsize=24)
    if "y_label" in config:
        plt.ylabel(config["y_label"], fontsize=24)
    if "legends" in config:
        plt.legend(config["legends"], loc=0, fontsize=20)

    # Removed xscale log
    if "yscale" in config and config["yscale"]:
        plt.yscale("log")

    plt.grid(True)

    if "x_ticks" in config:
        plt.xticks(config["x_ticks"])

    for label in ax.get_xticklabels():
        label.set_fontsize(20)
    for label in ax.get_yticklabels():
        label.set_fontsize(20)

    if "xlim" in config:
        plt.xlim(config["xlim"])

    plt.tight_layout()
    plt.show()

def draw_line_graph_multiple_with_x(X, lists, config):
    index = 0
    for i in range(len(lists)):
        x = X[i]
        y = lists[i]
        ln = plt.plot(x, y)
        if 'marker' in config:
            plt.setp(ln, linewidth=width[index], ls=styles[index], marker=marks[index],
                     markersize=marks_size[index], color=marker_color[index], markevery=gaps[index])
        else:
            plt.setp(ln, linewidth=width[index], ls=styles[index], color=marker_color[index])
        index += 1

    if "x_label" in config:
        plt.xlabel(config["x_label"], fontsize=24)
    if "y_label" in config:
        plt.ylabel(config["y_label"], fontsize=24)
    if "legends" in config:
        plt.legend(config["legends"], loc=0, fontsize=20)

    # Removed xscale log
    if "yscale" in config and config["yscale"]:
        plt.yscale("log")

    if "title" in config:
        plt.title(config["title"])

    plt.grid(True)

    if "x_ticks" in config:
        plt.xticks(config["x_ticks"])
    else:
        # Optional: Default linear ticks every 0.5 from 0 to 5
        plt.xticks([i * 0.1 for i in range(11)])

    for label in ax.get_xticklabels():
        label.set_fontsize(24)
    for label in ax.get_yticklabels():
        label.set_fontsize(24)

    if "xlim" in config:
        plt.xlim(config["xlim"])

    plt.tight_layout()

    if "output_path" in config:
        plt.savefig(config["output_path"], format='png', bbox_inches='tight')

    plt.show()
