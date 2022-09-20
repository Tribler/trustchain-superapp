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


if __name__ == "__main__":
    # encr_results()
    decr_results()
    # ac_verification_results()
