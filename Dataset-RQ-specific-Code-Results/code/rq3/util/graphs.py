import matplotlib.pyplot as plt

styles = ["-", "--", "-.", ":", "--", "--", "-.", ":"]
marks = ["^", "d", "o", "v", "p", "s", "<", ">"]
width = [7, 7, 7, 7, 6, 6, 6, 6]  
marks_size = [20, 15, 12, 14, 20, 10, 12, 15]
marker_color = ['#0F52BA', '#ff7518', '#6CA939', '#e34234', '#756bb1', 'brown', '#c994c7', '#636363']
gaps =  [5, 3, 2, 3, 5, 4, 4, 4]

def draw_line_graph_multiple(lists, config):
    fig, ax = plt.subplots(figsize=(8,6))
    index = 0
    for lst in lists:
        ln = ax.plot(range(1, len(lst) + 1), lst)
        plt.setp(ln, linewidth=width[index], ls=styles[index], color=marker_color[index])
        index += 1

    if "x_label" in config:
        ax.set_xlabel(config["x_label"], fontsize=24)
    if "y_label" in config:
        ax.set_ylabel(config["y_label"], fontsize=24)
    if "legends" in config:
        ax.legend(config["legends"], loc=0, fontsize=20)
    if "xscale" in config and config["xscale"]:
        ax.set_xscale("log")
    if "yscale" in config and config["yscale"]:
        ax.set_yscale("log")
    if "x_ticks" in config:
        ax.set_xticks(config["x_ticks"])
    if "xlim" in config:
        ax.set_xlim(config["xlim"])
    if "title" in config:
        ax.set_title(config["title"], fontsize=20)

    ax.grid(True)

    ax.tick_params(axis='x', labelsize=20)
    ax.tick_params(axis='y', labelsize=20)

    plt.tight_layout()

    if "output_path" in config:
        plt.savefig(config["output_path"], format='png', bbox_inches='tight')
        plt.close(fig)
    else:
        plt.show()

def draw_line_graph_multiple_with_x(X, lists, config):
    fig, ax = plt.subplots(figsize=(8,6))
    index = 0
    for i in range(len(lists)):
        y = lists[i]
        x = X[i]
        ln = ax.plot(x, y)
        if 'marker' in config and config['marker']:
            plt.setp(ln, linewidth=width[index], ls=styles[index], marker=marks[index], markersize=marks_size[index],
                    color=marker_color[index], markevery=gaps[index])
        else:
            plt.setp(ln, ls=styles[index], linewidth=width[index], color=marker_color[index])
        index += 1

    if "x_label" in config:
        ax.set_xlabel(config["x_label"], fontsize=24)
    if "y_label" in config:
        ax.set_ylabel(config["y_label"], fontsize=24)
    if "legends" in config:
        ax.legend(config["legends"], loc=0, fontsize=20)
    if "xscale" in config and config["xscale"]:
        ax.set_xscale("log")
    if "yscale" in config and config["yscale"]:
        ax.set_yscale("log")
    if "title" in config:
        ax.set_title(config["title"], fontsize=20)
    if "x_ticks" in config:
        ax.set_xticks(config["x_ticks"])
    if "xlim" in config:
        ax.set_xlim(config["xlim"])

    ax.grid(True)

    ax.tick_params(axis='x', labelsize=20)
    ax.tick_params(axis='y', labelsize=20)

    plt.tight_layout()

    if "output_path" in config:
        plt.savefig(config["output_path"], format='png', bbox_inches='tight')
        plt.close(fig)
    else:
        plt.show()
