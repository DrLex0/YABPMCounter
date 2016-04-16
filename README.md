# YABPMCounter
Bare-bones Android manual BPM counter

Release webpage: http://www.dr-lex.be/software/yabpmcounter.html

I was frustrated by the inaccuracy of existing BPM counters, so I wrote my own. As with my other
 apps, focus is on functionality and not on shiny design, so this app consists of nothing more than
 two buttons. Push the largest button at the rhythm of the music to start the counter.

The counter itself relies on the times between button pushes, and is updated upon every push (i.e.
 when the button is being pressed, not when it is released).
A two-stage filter is applied: first a weighted average, where the first 3 values get the same
 weight and the next ones an exponentially decaying weight. The result is appended to a second array
 and a median is calculated across the most recent values of this array.
There is a rudimentary protection against a missed button press, if the time after the last press is
 approximately double the previous time, the algorithm will insert a dummy value to compensate.
