# Import libraries
import matplotlib.pyplot as plt
import numpy as np


def encr_results():
    points = [x * 10 for x in range(1, 101)]
    cbc = [
        x / 1000
        for x in [
            88,
            60,
            76,
            94,
            118,
            133,
            148,
            166,
            187,
            203,
            222,
            240,
            270,
            278,
            296,
            314,
            333,
            351,
            371,
            402,
            406,
            424,
            451,
            462,
            499,
            507,
            519,
            540,
            552,
            577,
            591,
            611,
            627,
            644,
            663,
            688,
            735,
            757,
            742,
            770,
            792,
            797,
            825,
            831,
            855,
            869,
            887,
            915,
            929,
            946,
            966,
            980,
            1000,
            1013,
            1034,
            1053,
            1073,
            1095,
            1113,
            1127,
            1147,
            1170,
            1185,
            1202,
            1212,
            1233,
            1258,
            1278,
            1294,
            1308,
            1327,
            1344,
            1368,
            1396,
            1405,
            1421,
            1444,
            1458,
            1497,
            1497,
            1505,
            1528,
            1549,
            1572,
            1588,
            1602,
            1663,
            1663,
            1705,
            1675,
            1700,
            1720,
            1787,
            1754,
            1767,
            1805,
            1805,
            1830,
            1839,
            1868,
        ]
    ]
    ctr = [
        x / 1000
        for x in [
            27,
            46,
            42,
            47,
            52,
            61,
            69,
            78,
            88,
            96,
            163,
            115,
            192,
            202,
            142,
            150,
            158,
            167,
            175,
            184,
            192,
            338,
            349,
            362,
            422,
            394,
            253,
            258,
            271,
            274,
            466,
            479,
            498,
            311,
            317,
            358,
            362,
            345,
            355,
            365,
            374,
            380,
            390,
            399,
            403,
            412,
            424,
            436,
            439,
            448,
            458,
            470,
            476,
            481,
            492,
            500,
            509,
            519,
            528,
            537,
            547,
            556,
            561,
            564,
            572,
            582,
            594,
            611,
            624,
            616,
            628,
            636,
            642,
            654,
            661,
            677,
            683,
            691,
            700,
            718,
            710,
            721,
            730,
            740,
            743,
            769,
            769,
            785,
            781,
            794,
            804,
            810,
            833,
            836,
            832,
            841,
            846,
            859,
            866,
            921,
        ]
    ]

    fig = plt.figure(figsize=(10, 7))
    fig.suptitle("CTR vs CBC encryption", fontsize=14, fontweight="bold")

    ax = fig.add_subplot()

    ax.plot(
        points, cbc, "r--", points, ctr, "b--", points, cbc, "r.", points, ctr, "b."
    )
    ax.axis([0, 1000, 0, 2.5])

    ax.set_xlabel("MB")
    ax.set_ylabel("Seconds")

    plt.show()


def decr_results():
    points = [x * 10 for x in range(1, 101)]
    cbc = [
        x / 1000
        for x in [
            122,
            51,
            72,
            60,
            74,
            84,
            92,
            102,
            117,
            124,
            135,
            146,
            157,
            167,
            181,
            190,
            200,
            331,
            224,
            237,
            246,
            261,
            272,
            301,
            461,
            310,
            316,
            325,
            338,
            351,
            361,
            565,
            380,
            386,
            406,
            413,
            425,
            436,
            446,
            460,
            467,
            474,
            497,
            504,
            513,
            526,
            532,
            550,
            557,
            573,
            575,
            589,
            604,
            606,
            616,
            633,
            643,
            659,
            666,
            675,
            1016,
            708,
            709,
            722,
            737,
            741,
            759,
            768,
            776,
            787,
            803,
            821,
            817,
            845,
            851,
            1330,
            1359,
            880,
            886,
            898,
            914,
            939,
            928,
            936,
            960,
            1502,
            987,
            1006,
            999,
            998,
            1017,
            1047,
            1054,
            1050,
            1057,
            1073,
            1111,
            1110,
            1120,
            1128,
        ]
    ]
    ctr = [
        x / 1000
        for x in [
            64,
            33,
            37,
            56,
            56,
            61,
            71,
            80,
            91,
            97,
            110,
            114,
            125,
            131,
            143,
            152,
            159,
            167,
            176,
            184,
            197,
            204,
            215,
            220,
            230,
            238,
            249,
            256,
            432,
            455,
            278,
            286,
            295,
            506,
            313,
            323,
            337,
            338,
            349,
            355,
            365,
            371,
            384,
            393,
            401,
            407,
            420,
            426,
            435,
            445,
            451,
            464,
            470,
            474,
            482,
            496,
            854,
            520,
            519,
            536,
            538,
            549,
            554,
            569,
            579,
            587,
            592,
            603,
            610,
            624,
            629,
            632,
            640,
            662,
            666,
            668,
            679,
            691,
            696,
            704,
            712,
            724,
            728,
            741,
            745,
            754,
            768,
            833,
            781,
            785,
            806,
            814,
            816,
            836,
            841,
            1402,
            857,
            867,
            873,
            890,
        ]
    ]

    fig = plt.figure(figsize=(10, 7))
    fig.suptitle("CTR vs CBC decryption", fontsize=14, fontweight="bold")

    ax = fig.add_subplot()

    ax.plot(
        points, cbc, "r--", points, ctr, "b--", points, cbc, "ro", points, ctr, "bo"
    )
    ax.axis([0, 1000, 0, 2])

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


ST = "SESSION_TOKEN"
TCID = "TCID"
EBSI = "JWT"
BASELINE = "JSONLD"


def to_sec(data):
    return [x / 1000 for x in data]


def single_test():
    res = {
        "SESSION_TOKEN": [
            1002,
            837,
            974,
            766,
            1035,
            866,
            711,
            723,
            575,
            782,
            733,
            863,
            651,
            # 5416,
            751,
            1035,
            808,
            616,
            671,
            # 7263,
            # 2709,
            609,
            723,
            611,
            640,
        ],
        "TCID": [
            1949,
            2126,
            2078,
            1795,
            # 3060,
            1608,
            2053,
            1890,
            2415,
            2703,
            # 4504,
            2731,
            2698,
            1928,
            # 4583,
            2676,
            2736,
            2471,
            2381,
            2529,
            # 7081,
            2645,
            # 4606,
            1914,
            2074,
        ],
        "JWT": [
            # 4593,
            1650,
            # 5972,
            2134,
            1445,
            # 6433,
            2090,
            1579,
            # 5629,
            1532,
            1450,
            1552,
            1234,
            1454,
            # 5967,
            1955,
            1442,
            1405,
            1273,
            1695,
            1222,
            1289,
            1289,
            1783,
            1273,
        ],
        "JSONLD": [
            # 4139,
            780,
            578,
            798,
            963,
            # 3958,
            1028,
            1207,
            # 8260,
            # 3800,
            719,
            1267,
            1428,
            # 4989,
            495,
            787,
            1416,
            1332,
            1271,
            1026,
            732,
            749,
            698,
            563,
            1050,
        ],
    }
    return res


def single_test_4g():
    res = {
        "SESSION_TOKEN": [
            1987,
            1903,
            2698,
            1536,
            2461,
            2214,
            2556,
            2729,
            2413,
            2208,
            1615,
            1606,
            2501,
            1857,
            1454,
            # 3012,
            1461,
            1919,
            2138,
            1723,
            1553,
            1904,
            1677,
            1896,
            1625,
        ],
        "TCID": [
            3696,
            # 4277,
            3430,
            # 4435,
            3239,
            3752,
            3391,
            3564,
            3427,
            # 5675,
            3653,
            # 7991,
            3678,
            3745,
            3308,
            # 4243,
            3260,
            3154,
            3205,
            3463,
            3247,
            # 7455,
            3607,
            3362,
            3394,
        ],
        "JWT": [
            2692,
            2414,
            2187,
            2297,
            2325,
            2041,
            2199,
            2727,
            2415,
            2558,
            2200,
            # 3422,
            2417,
            2372,
            2100,
            # 3056,
            2494,
            2614,
            2239,
            2536,
            2243,
            2418,
            2149,
            # 3007,
            2367,
        ],
        "JSONLD": [
            2610,
            1442,
            2442,
            1521,
            1520,
            2168,
            1560,
            1692,
            1385,
            1690,
            2380,
            1456,
            1448,
            1601,
            1541,
            2213,
            # 6123,
            1789,
            1594,
            1510,
            1462,
            3086,
            1965,
            # 3447,
            2768,
        ],
    }
    return res


def double_5_1():
    res = {
        "SESSION_TOKEN": [
            1833,
            6316,
            2008,
            4704,
            2033,
            4234,
            1653,
            6093,
            1751,
            1458,
            2026,
            1467,
            1313,
            8873,
            4807,
            1654,
            4901,
            1216,
            5974,
            1513,
            4946,
            6207,
            1798,
            4922,
            1126,
        ],
        "TCID": [
            2880,
            2813,
            3153,
            3106,
            3236,
            3199,
            5258,
            2686,
            2884,
            3621,
            3173,
            8366,
            5028,
            5190,
            4731,
            4371,
            5346,
            6178,
            5647,
            5594,
            5185,
            4584,
            6470,
            6305,
            8141,
        ],
        "JWT": [
            3615,
            1155,
            2660,
            1063,
            1115,
            1354,
            1489,
            990,
            1158,
            1267,
            1246,
            1113,
            1096,
            1191,
            1111,
            1708,
            1238,
            1163,
            1066,
            1304,
            1254,
            1405,
            1218,
            1708,
            1138,
        ],
        "JSONLD": [
            3707,
            1372,
            848,
            3342,
            824,
            940,
            716,
            719,
            921,
            3761,
            4044,
            1220,
            825,
            1453,
            723,
            1005,
            750,
            3439,
            750,
            969,
            762,
            1133,
            746,
            923,
            844,
        ],
    }
    return res


def ac_ver_single():
    res_wifi = single_test()

    data_wifi = [
        to_sec(res_wifi[BASELINE]),
        to_sec(res_wifi[ST]),
        to_sec(res_wifi[TCID]),
        to_sec(res_wifi[EBSI]),
    ]

    res_4g = single_test_4g()

    data_4g = [
        to_sec(res_4g[BASELINE]),
        to_sec(res_4g[ST]),
        to_sec(res_4g[TCID]),
        to_sec(res_4g[EBSI]),
    ]

    mean_wifi = [
        np.mean(res_wifi[BASELINE]),
        np.mean(res_wifi[ST]),
        np.mean(res_wifi[TCID]),
        np.mean(res_wifi[EBSI]),
    ]

    mean_4g = [
        np.mean(res_4g[BASELINE]),
        np.mean(res_4g[ST]),
        np.mean(res_4g[TCID]),
        np.mean(res_4g[EBSI]),
    ]

    print(mean_wifi)
    print(mean_4g)

    diff = [(mean_4g[i] - w) / w for i, w in enumerate(mean_wifi)]
    print(diff)

    fig = plt.figure(figsize=(10, 7))
    ax = fig.add_subplot()

    wifi_plot = ax.boxplot(
        data_wifi,
        positions=np.array(np.arange(len(data_wifi))) * 2.0 - 0.35,
        widths=0.6,
    )
    g4_plot = ax.boxplot(
        data_4g, positions=np.array(np.arange(len(data_4g))) * 2.0 + 0.35, widths=0.6
    )

    # ax.boxplot(data)
    ax.set_title("File transfer time (220kB) - Single requester")
    # ax.set_xlabel('xlabel')
    ax.set_ylabel("Seconds")

    ticks = ["Baseline", "Session token", "TCID", "EBSI VC"]
    # ax.set_xticks([1, 2, 3, 4], ticks)

    def define_box_properties(plot_name, color_code, label):
        for k, v in plot_name.items():
            plt.setp(plot_name.get(k), color=color_code)

        # use plot function to draw a small line to name the legend.
        plt.plot([], c=color_code, label=label)
        plt.legend()

    # setting colors for each groups
    define_box_properties(wifi_plot, "#D7191C", "WiFi")
    define_box_properties(g4_plot, "#2C7BB6", "4G")

    # set the x label values
    plt.xticks(np.arange(0, len(ticks) * 2, 2), ticks)

    # set the limit for x axis
    plt.xlim(-2, len(ticks) * 2)

    # Creating plot
    # plt.boxplot(data)

    # show plot
    plt.show()


if __name__ == "__main__":
    # encr_results()
    # decr_results()
    # ac_verification_results()
    ac_ver_single()
