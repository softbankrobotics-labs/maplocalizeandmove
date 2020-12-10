package com.softbankrobotics.maplocalizeandmove.Utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import com.softbankrobotics.dx.pepperextras.ui.ExplorationMapView

class PointsOfInterestView(context: Context, attributeSet: AttributeSet): ExplorationMapView(context, attributeSet){
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mapFramePosition?.let {
            canvas.drawCircle(it.x, it.y, poiCircleSize.toFloat(), mapFramePaint)
        }

        poiPositions?.let {
            for (poiPosition in it){
                canvas.drawCircle(poiPosition.x, poiPosition.y, poiCircleSize.toFloat(), poiPaint)
            }
        }
    }

    private val TAG = "MSI_PointsOfInterestView"

    // Size of the circle representing the robot
    private val poiCircleSize: Int = 10

    // Paint for the MapFrame
    private val mapFramePaint =  Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    // Paint for the Poi
    private val poiPaint =  Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }


    private var mapFramePosition : PointF? = null
    private var poiPositions : MutableList<PointF>? = mutableListOf<PointF>()

    fun setMapFramPosition() {
        // Compute the position of the robot in the View
        mapFramePosition = mapToViewCoordinates(0.0, 0.0)
        Log.i(TAG, "setMapFramPosition: x: ${mapFramePosition?.x}, y: ${mapFramePosition?.y}")
        postInvalidate()
    }

    fun setPoiPositions(positionsInMap: MutableList<PointF>) {
        for (position in positionsInMap){
            poiPositions?.add(mapToViewCoordinates(position.x.toDouble(),position.y.toDouble()))
        }
        for (position in poiPositions!!){
            Log.i(TAG, "setPoiPositions: x: ${position.x}, y: ${position.y} ")
        }
        postInvalidate()
    }
}