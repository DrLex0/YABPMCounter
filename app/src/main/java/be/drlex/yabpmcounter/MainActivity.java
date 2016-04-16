/*
Copyright (c) 2015, Alexander Thomas
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package be.drlex.yabpmcounter;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.ArrayDeque;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity {
    private Button bpmButton, resetButton;
    private long lastButtonPushTime;
    private ArrayDeque<Long> timingValuesDeque;
    private ArrayDeque<Float> bpmValuesDeque;
    private final long timing_deque_size = 16;
    private final long median_deque_size = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lastButtonPushTime = 0;
        timingValuesDeque = new ArrayDeque<>();
        bpmValuesDeque = new ArrayDeque<>();

        bpmButton = (Button) findViewById(R.id.bpm_button);
        bpmButton.setOnTouchListener(onBpmButtonTouch);
        resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(onResetButtonClick);
        // For a longer title in the title bar.
        // (From API level 11 on, getActionBar can be used)
        getSupportActionBar().setTitle(R.string.title_activity_main);
    }

    private View.OnTouchListener onBpmButtonTouch = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent motionEvent) {
            if (motionEvent.getAction() != MotionEvent.ACTION_DOWN)
                return false;

            long currentTime = System.currentTimeMillis();
            if (lastButtonPushTime == 0) {
                lastButtonPushTime = currentTime;
                return true;
            }
            Long timeElapsed = Long.valueOf(currentTime - lastButtonPushTime);
            lastButtonPushTime = currentTime;

            // Detect missed beat: current measurement appears twice the last one
            if (timingValuesDeque.size() > 0 && timeElapsed > 1.75 * timingValuesDeque.getFirst() &&
                    timeElapsed < 2.3 * timingValuesDeque.getFirst()) {
                // Insert half the value twice to fill in the missed beat
                timeElapsed /= 2;
                timingValuesDeque.addFirst(timeElapsed);
            }
            timingValuesDeque.addFirst(timeElapsed);
            while (timingValuesDeque.size() > timing_deque_size)
                timingValuesDeque.removeLast();

            float weightedAverageTime = decayingWeightedAverageFromDeque(timingValuesDeque);
            float bpmEstimate = 60000.0F / weightedAverageTime;

            // Calculate median of the set of previous weighted averages
            bpmValuesDeque.addFirst(bpmEstimate);
            while (bpmValuesDeque.size() > median_deque_size)
                bpmValuesDeque.removeLast();

            long bpmMedian = Math.round(medianFromFloatDeque(bpmValuesDeque));

            bpmButton.setText(String.format("%.1f", bpmEstimate) + "\n" + getString(R.string.median)
                    + ": " + String.valueOf(bpmMedian));
            return true;
        }
    };

    private View.OnClickListener onResetButtonClick = new View.OnClickListener() {
        public void onClick(View v) {
            timingValuesDeque.clear();
            bpmValuesDeque.clear();
            lastButtonPushTime = 0;
            bpmButton.setText(R.string.bpm_hitme);
        }
    };

    private float decayingWeightedAverageFromDeque(ArrayDeque<Long> deque) {
        float weightedAverage = 0, weight = 1, totalWeight = 0;
        int valuesAdded = 0;
        // Weighted average: exponentially decrease the weight after the third value
        for (Long value : deque) {
            weightedAverage += (Long) value * weight;
            totalWeight += weight;
            valuesAdded++;
            if (valuesAdded > 3)
                weight *= .8;
        }
        if (totalWeight == 0)
            return weightedAverage;
        return weightedAverage / totalWeight;
    }

    private float medianFromFloatDeque(ArrayDeque<Float> deque) {
        float bpmValuesArray[] = new float[bpmValuesDeque.size()];
        int i = 0;
        for (Float value : bpmValuesDeque) {
            bpmValuesArray[i] = value;
            i++;
        }
        Arrays.sort(bpmValuesArray);
        int arrayLength = bpmValuesArray.length;
        if (arrayLength % 2 == 0)
            return (bpmValuesArray[arrayLength / 2] + bpmValuesArray[arrayLength / 2 - 1]) / 2;
        else
            return bpmValuesArray[arrayLength / 2];
    }
}
