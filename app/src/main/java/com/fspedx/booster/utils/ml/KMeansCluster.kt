package com.fspedx.booster.utils.ml

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * K-Means clustering implementation for touch data.
 * Groups touch points into clusters to find button centroids.
 */
object KMeansCluster {

    /**
     * Represents a touch point with X and Y coordinates.
     */
    data class TouchPoint(val x: Float, val y: Float)

    /**
     * Represents a cluster with a centroid and a list of points.
     */
    data class Cluster(val centroid: TouchPoint, val points: List<TouchPoint>)

    /**
     * Find K centroids from a list of touch points.
     * @param points List of touch points.
     * @param k Number of clusters (buttons) to find.
     * @param maxIterations Maximum iterations for convergence.
     * @param convergenceThreshold Distance threshold for convergence.
     * @return List of centroids (TouchPoint).
     */
    fun findCentroids(
        points: List<TouchPoint>,
        k: Int,
        maxIterations: Int = 100,
        convergenceThreshold: Float = 0.1f
    ): List<TouchPoint> {
        if (points.isEmpty() || k <= 0) {
            return emptyList()
        }

        if (k >= points.size) {
            return points.take(k)
        }

        // Initialize centroids using K-Means++ algorithm
        val centroids = mutableListOf<TouchPoint>()
        
        // First centroid: random point
        centroids.add(points[Random.nextInt(points.size)])
        
        // Remaining centroids: pick points with max distance from existing centroids
        for (i in 1 until k) {
            val distances = points.map { point ->
                centroids.minOf { centroid ->
                    distanceSquared(point, centroid)
                }
            }
            
            val totalDistance = distances.sum()
            if (totalDistance == 0f) break
            
            // Weighted random selection
            var randomValue = Random.nextFloat() * totalDistance
            var selectedIndex = 0
            for ((index, distance) in distances.withIndex()) {
                randomValue -= distance
                if (randomValue <= 0) {
                    selectedIndex = index
                    break
                }
            }
            
            centroids.add(points[selectedIndex])
        }

        // Run K-Means iterations
        var iteration = 0
        var hasConverged = false
        var previousCentroids = centroids.toList()

        while (iteration < maxIterations && !hasConverged) {
            // Assign points to nearest centroid
            val clusters = assignPointsToClusters(points, centroids)
            
            // Update centroids
            val newCentroids = clusters.map { cluster ->
                if (cluster.points.isEmpty()) {
                    // If cluster is empty, keep the old centroid
                    cluster.centroid
                } else {
                    val avgX = cluster.points.map { it.x }.average().toFloat()
                    val avgY = cluster.points.map { it.y }.average().toFloat()
                    TouchPoint(avgX, avgY)
                }
            }
            
            // Check convergence
            var totalMovement = 0f
            for (i in centroids.indices) {
                totalMovement += distance(centroids[i], newCentroids[i])
            }
            
            if (totalMovement < convergenceThreshold) {
                hasConverged = true
            }
            
            previousCentroids = centroids.toList()
            centroids.clear()
            centroids.addAll(newCentroids)
            iteration++
        }

        return centroids
    }

    /**
     * Assign each point to the nearest centroid.
     */
    private fun assignPointsToClusters(
        points: List<TouchPoint>,
        centroids: List<TouchPoint>
    ): List<Cluster> {
        val clusters = centroids.map { Cluster(it, mutableListOf()) }
        
        for (point in points) {
            var minDistance = Float.MAX_VALUE
            var closestIndex = 0
            
            for ((index, centroid) in centroids.withIndex()) {
                val dist = distanceSquared(point, centroid)
                if (dist < minDistance) {
                    minDistance = dist
                    closestIndex = index
                }
            }
            
            (clusters[closestIndex].points as MutableList<TouchPoint>).add(point)
        }
        
        return clusters
    }

    /**
     * Calculate Euclidean distance between two points.
     */
    private fun distance(point1: TouchPoint, point2: TouchPoint): Float {
        return sqrt(distanceSquared(point1, point2))
    }

    /**
     * Calculate squared Euclidean distance between two points.
     * More efficient than distance() for comparisons.
     */
    private fun distanceSquared(point1: TouchPoint, point2: TouchPoint): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        return dx * dx + dy * dy
    }

    /**
     * Find the optimal number of clusters using the Elbow method.
     * @param points List of touch points.
     * @param maxK Maximum number of clusters to try.
     * @return The optimal K value.
     */
    fun findOptimalK(points: List<TouchPoint>, maxK: Int = 10): Int {
        if (points.isEmpty()) return 0
        
        val distortions = mutableListOf<Double>()
        
        for (k in 1..minOf(maxK, points.size)) {
            val centroids = findCentroids(points, k, maxIterations = 50)
            var distortion = 0.0
            
            for (point in points) {
                val minDist = centroids.minOf { centroid ->
                    distanceSquared(point, centroid).toDouble()
                }
                distortion += minDist
            }
            
            distortions.add(distortion)
        }
        
        // Find the "elbow" point
        if (distortions.size < 2) return 1
        
        // Calculate the rate of change
        val rates = mutableListOf<Double>()
        for (i in 1 until distortions.size) {
            val rate = (distortions[i - 1] - distortions[i]) / (distortions[0] - distortions.last())
            rates.add(rate)
        }
        
        // Find where the rate drops below a threshold (simple heuristic)
        val threshold = 0.5
        for ((index, rate) in rates.withIndex()) {
            if (rate < threshold) {
                return index + 1
            }
        }
        
        return if (distortions.size > 4) 4 else distortions.size
    }
}
