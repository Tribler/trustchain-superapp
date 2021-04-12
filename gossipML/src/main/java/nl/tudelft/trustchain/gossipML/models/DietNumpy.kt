package nl.tudelft.trustchain.gossipML.models

operator fun Array<Double>.plus(other: Double): Array<Double> {
    return this.map { it + other }.toTypedArray()
}

operator fun Double.plus(doubles: Array<Double>): Array<Double> {
    return doubles + this
}

operator fun Array<Double>.minus(other: Double): Array<Double> {
    return this.map { it - other }.toTypedArray()
}

operator fun Double.minus(doubles: Array<Double>): Array<Double> {
    return doubles - this
}

operator fun Array<Double>.times(other: Double): Array<Double> {
    return this.map { it * other }.toTypedArray()
}

operator fun Double.times(doubles: Array<Double>): Array<Double> {
    return doubles * this
}

operator fun Array<Double>.div(other: Double): Array<Double> {
    return this.map { it / other }.toTypedArray()
}

operator fun Double.div(doubles: Array<Double>): Array<Double> {
    return doubles / this
}

operator fun Array<Double>.plus(other: Array<Double>): Array<Double> {
    return this.zip(other).map { (i1, i2) -> i1 + i2 }.toTypedArray()
}

operator fun Array<Double>.minus(other: Array<Double>): Array<Double> {
    return this.zip(other).map { (i1, i2) -> i1 - i2 }.toTypedArray()
}

operator fun Array<Double>.times(other: Array<Double>): Double {
    return this.zip(other).map { (i1, i2) -> i1 * i2 }.toTypedArray().sum()
}
