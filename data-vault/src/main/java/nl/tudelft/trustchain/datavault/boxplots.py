# Import libraries
import matplotlib.pyplot as plt
import numpy as np


def encr_results():
    points = [10, 100, 500, 1000, 5000, 10000]
    cbc = [x / 1000 for x in [71, 202, 916, 1778, 8686, 17375]]
    ctr = [x / 1000 for x in [21, 92, 416, 798, 3905, 7792]]

    fig = plt.figure(figsize=(10, 7))
    fig.suptitle("CTR vs CBC encryption", fontsize=14, fontweight="bold")

    ax = fig.add_subplot()

    ax.plot(
        points, cbc, "r--", points, ctr, "b--", points, cbc, "ro", points, ctr, "bo"
    )
    ax.axis([0, 10000, 0, 19])

    ax.set_xlabel("MB")
    ax.set_ylabel("Seconds")

    plt.show()


def decr_results():
    points = [10, 100, 500, 1000, 5000, 10000]
    cbc = [x / 1000 for x in [52, 128, 592, 1115, 5453, 10913]]
    ctr = [x / 1000 for x in [32, 98, 437, 866, 4365, 8530]]

    fig = plt.figure(figsize=(10, 7))
    fig.suptitle("CTR vs CBC decryption", fontsize=14, fontweight="bold")

    ax = fig.add_subplot()

    ax.plot(
        points, cbc, "r--", points, ctr, "b--", points, cbc, "ro", points, ctr, "bo"
    )
    ax.axis([0, 10000, 0, 19])

    ax.set_xlabel("MB")
    ax.set_ylabel("Seconds")

    plt.show()


def matrix(a, b):
    result = []
    for x in a:
        for y in b:
            result.append(x + y)
    return result


def ac_verification_results():
    access_verification = 3
    transfer_speed = [1553, 1568, 1675, 1964]
    transfer_speed_prime = [1530, 1598, 1610, 1675, 1890]
    tcid = [0, 1, 1, 2]
    ebsi = [253, 255, 285, 350]

    ts_mean = np.mean(transfer_speed_prime)
    ts_std = np.std(transfer_speed_prime)

    np.random.seed(23)
    rand_ts = np.random.normal(ts_mean, ts_std, 5)
    # print(np.random.normal(ts_mean, ts_std, 5))

    no_ver = [1731.2173587, 1553.99507121, 1761.70323018, 1595.89155109, 1686.24472596]
    tcid_ts = [
        1614.95392095,
        1786.89431232,
        1588.01983542,
        1786.02733512,
        1690.54875109,
    ]
    ebsi_ts = [1571.15642809, 1615.17628684, 1690.54875109, 1520.02618, 1661.1838234]

    data = [
        transfer_speed,
        no_ver,
        matrix(tcid_ts, tcid),
        matrix(ebsi_ts, ebsi),
    ]

    fig = plt.figure(figsize=(10, 7))
    ax = fig.add_subplot()
    ax.boxplot(data)
    ax.set_title("File request speed (500kB)")
    # ax.set_xlabel('xlabel')
    ax.set_ylabel("Milliseconds")
    ax.set_xticks([1, 2, 3, 4], ["Baseline", "Session token", "TCID", "EBSI VC"])

    # Creating plot
    # plt.boxplot(data)

    # show plot
    plt.show()


if __name__ == "__main__":
    # encr_results()
    # decr_results()
    ac_verification_results()
