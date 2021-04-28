package nl.tudelft.trustchain.gossipML.models

import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.MatrixFactorization
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.PublicMatrixFactorization
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.SongFeature
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class ColabFilterTest {

    @Test
    fun testCreateAndUpdate() {
        val model = MatrixFactorization(Array(0) { "" }.zip(Array(0) { 0.0 }).toMap().toSortedMap())

        model.merge(
            sortedMapOf(
                Pair("a", SongFeature(5.0, Array(5) { Random.nextDouble(1.0, 5.0) }, 0.0)),
                Pair("b", SongFeature(2.0, Array(5) { Random.nextDouble(0.0, 3.0) }, 0.0)),
                Pair("c", SongFeature(7.0, Array(5) { Random.nextDouble(0.0, 8.0) }, 0.0)),
            )
        )

        Assert.assertThat(model, instanceOf(MatrixFactorization::class.java))
        Assert.assertEquals(model.songFeatures.size, 3)
    }

    @Test
    fun testPredictions() {
        val pubModel = PublicMatrixFactorization(
            sortedMapOf(
                Pair("good", SongFeature(1.0, Array(5) { Random.nextDouble(2.0, 5.0) }, 0.0)),
                Pair("bad", SongFeature(1.0, Array(5) { 1.0 }, 0.0)), // nobody likes this song
            )
        )
        val model = MatrixFactorization(pubModel)
        val pred = model.predict()
        Assert.assertEquals(pred, "good")
    }

    @Test
    fun testRatingsUpdate() {
        val model = MatrixFactorization(
            sortedMapOf(
                Pair("good", SongFeature(1.0, Array(5) { Random.nextDouble(2.0, 5.0) }, 0.0)),
                Pair("bad", SongFeature(1.0, Array(5) { 1.0 }, 0.0)), // nobody likes this song
            )
        )

        model.updateRatings(
            sortedMapOf(
                Pair("good", 0.0),
                Pair("bad", 1.0)
            )
        )

        Assert.assertNotEquals(model.songFeatures["bad"], Array(5) { 0.0 })
    }

    @Test
    fun testSyncModels() {
        val pubModel = PublicMatrixFactorization(
            sortedMapOf(
                Pair("good", SongFeature(1.0, Array(5) { Random.nextDouble(2.0, 5.0) }, 0.0)),
                Pair("bad", SongFeature(1.0, Array(5) { 1.0 }, 0.0)), // nobody likes this song
            )
        )
        val models = Array(3) { MatrixFactorization(pubModel) }

        val updatedModel = MatrixFactorization(pubModel)
        updatedModel.updateRatings(sortedMapOf(Pair("new", 5.0)))

        // gossip new song to peers
        for (model in models) {
            model.merge(PublicMatrixFactorization(updatedModel.songFeatures))
            Assert.assertTrue(model.songFeatures.keys.contains("new"))
        }
    }

    private fun pairwiseDifference(models: Array<MatrixFactorization>): Double {
        var diff = 0.0
        var total = 0.0
        for (m1 in models) {
            for (m2 in models) {
                if (m1 === m2) continue
                for (k in m1.songFeatures.keys) {
                    diff += abs((m1.songFeatures[k]!!.feature - m2.songFeatures[k]!!.feature).sum())
                    total += (m1.songFeatures[k]!!.feature + m2.songFeatures[k]!!.feature).sum() / 2
                }
            }
        }
        return diff / total
    }

    @Test
    fun testGossipConvergence() {
        val models = arrayOf(
            // a fans
            MatrixFactorization(sortedMapOf(Pair("a", 25.0), Pair("aa", 30.0), Pair("aaa", 27.0), Pair("b", 1.0), Pair("cc", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("a", 28.0), Pair("aa", 29.0), Pair("dd", 1.0), Pair("bbb", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("aaa", 21.0), Pair("aa", 23.0), Pair("c", 1.0), Pair("d", 1.0))),
            // b fans
            MatrixFactorization(sortedMapOf(Pair("b", 25.0), Pair("bb", 29.0), Pair("bbb", 24.0), Pair("a", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("bb", 26.0), Pair("b", 28.0), Pair("d", 3.0), Pair("dd", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("bbb", 27.0), Pair("b", 27.0), Pair("c", 2.0), Pair("aa", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("bb", 28.0), Pair("bbb", 26.0), Pair("ccc", 1.0), Pair("aaa", 1.0))),
            // c fans
            MatrixFactorization(sortedMapOf(Pair("c", 21.0), Pair("cc", 26.0), Pair("ccc", 27.0), Pair("bbb", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("cc", 22.0), Pair("c", 25.0), Pair("bb", 1.0), Pair("d", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("ccc", 23.0), Pair("c", 24.0), Pair("b", 2.0), Pair("aa", 1.0), Pair("aaa", 1.0))),
            // d fan
            MatrixFactorization(sortedMapOf(Pair("d", 30.0), Pair("dd", 30.0), Pair("aaa", 1.0), Pair("bb", 1.0), Pair("c", 1.0))),
        )

        // gossip models iteratively until (hopefully) convergence
        for (round in 1..10) {
            for (m1 in models) {
                for (m2 in models) {
                    if (m1 === m2) continue
                    m2.merge(PublicMatrixFactorization(m1.songFeatures.toSortedMap()))
                }
            }
        }

        // models never converge completely because update() is called after merging
        Assert.assertTrue(pairwiseDifference(models) < 0.1)
    }

    @Test
    fun testRecommendations() {
        // high play counts within group, low play counts outside
        val models = arrayOf(
            // a fans
            MatrixFactorization(sortedMapOf(Pair("a", 25.0), Pair("aa", 30.0), Pair("aaa", 27.0), Pair("b", 1.0), Pair("cc", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("a", 28.0), Pair("aa", 29.0), Pair("dd", 1.0), Pair("bbb", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("aaa", 21.0), Pair("aa", 23.0), Pair("c", 1.0), Pair("d", 1.0))),
            // b fans
            MatrixFactorization(sortedMapOf(Pair("b", 25.0), Pair("bb", 29.0), Pair("bbb", 24.0), Pair("a", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("bb", 26.0), Pair("b", 28.0), Pair("d", 3.0), Pair("dd", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("bbb", 27.0), Pair("b", 27.0), Pair("c", 2.0), Pair("aa", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("bb", 28.0), Pair("bbb", 26.0), Pair("ccc", 1.0), Pair("aaa", 1.0))),
            // c fans
            MatrixFactorization(sortedMapOf(Pair("c", 21.0), Pair("cc", 26.0), Pair("ccc", 27.0), Pair("bbb", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("cc", 22.0), Pair("c", 25.0), Pair("bb", 1.0), Pair("d", 1.0))),
            MatrixFactorization(sortedMapOf(Pair("ccc", 23.0), Pair("c", 24.0), Pair("b", 2.0), Pair("aa", 1.0), Pair("aaa", 1.0))),
            // d fan
            MatrixFactorization(sortedMapOf(Pair("d", 30.0), Pair("dd", 30.0), Pair("aaa", 1.0), Pair("bb", 1.0), Pair("c", 1.0))),
        )

        // gossip models iteratively until convergence
        for (round in 1..30) {
            for (m1 in models) {
                for (m2 in models) {
                    if (m1 === m2) continue
                    m2.merge(PublicMatrixFactorization(m1.songFeatures.toSortedMap()))
                }
            }
        }

        // often there's still 1 error, but 87% is good enough for this test set
        val numCorrect = arrayOf(
            "aaa" == models[1].predict(),
            "a" == models[2].predict(),
            "bbb" == models[4].predict(),
            "bb" == models[5].predict(),
            "b" == models[6].predict(),
            "ccc" == models[8].predict(),
            "cc" == models[9].predict(),
            "b" == models[7].predict()
        ).map { if (it) 1 else 0 }.sum()

        Assert.assertTrue(numCorrect >= 5)
    }
}
