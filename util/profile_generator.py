import os
import json
import sys
import random
from torrentool.api import Torrent

from datetime import timedelta, datetime

# dates for random uplaod time generation
START_DATE = datetime.strptime("1/1/2023", "%d/%m/%Y")
END_DATE = datetime.strptime("25/4/2023", "%d/%m/%Y")


def random_date(start, end):
    """
    This function will return a random datetime between two datetime
    objects.
    """
    delta = end - start
    int_delta = (delta.days * 24 * 60 * 60) + delta.seconds
    random_second = random.randrange(int_delta)
    return start + timedelta(seconds=random_second)


def get_random_torrents() -> list[Torrent]:
    torrent_list = os.listdir("torrents/")

    # randomly select at most half of the available torrents
    num_torrents = random.randint(1, len(torrent_list) // 2)

    selected_torrents = random.sample(torrent_list, num_torrents)

    to_return = [Torrent.from_file("torrents/" + i) for i in selected_torrents]

    return to_return


def main():
    try:
        num_nodes = int(sys.argv[1])
    except:
        print("Usage: python3 profile_generator.py <num_nodes>")
        exit(1)

    output = {"nodes": []}

    for _ in range(num_nodes):
        node = {"profiles": []}
        torrents = get_random_torrents()

        for t in torrents:
            hop_count = random.randint(1, 10000)
            times_seen = random.randint(1, 1000)
            likes = random.randint(0, 1000)
            watched = random.randint(0, 1) == 1
            watch_time = random.randint(0, 1000)
            upload_date = int(
                random_date(START_DATE, END_DATE).timestamp() * 1000
            )  # date in ms

            profile = {
                "magnet": t.magnet_link,
                "watched": watched,
                "watchTime": watch_time,
                "uploadDate": upload_date,
                "hopCount": hop_count,
                "timesSeen": times_seen,
                "likes": likes,
            }
            node["profiles"].append(profile)

        output["nodes"].append(node)

    with open("nodes.json", "w") as f:
        json_output = json.dumps(output, indent=2)

        f.write(json_output)
        f.close()


if __name__ == "__main__":
    main()
