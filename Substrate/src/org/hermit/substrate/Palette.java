
/**
 * Substrate: grow crystal-like lines on a computational substrate
 *
 *       Lines like crystals grow on a computational substrate.  A  simple  per-
       pendicular growth rule creates intricate city-like structures.  Option-
       ally, cracks may also be circular, producing a cityscape more  familiar
       to places for which city planning is a distant, theoretical concern.

       Ported from the code by j.tarbell at http://complexification.net

       Copyright  ©  2003   by   J.   Tarbell   (complex@complexification.net,
       http://www.complexification.net).

       Ported      to      XScreensaver      2004      by     Mike     Kershaw
       (dragorn@kismetwireless.net)

AUTHOR
       J. Tarbell <complex@complexification.net>, Jun-03

       Mike Kershaw <dragorn@kismetwireless.net>, Oct-04


 * This is an Android implementation of the KDE game "knetwalk" by
 * Andi Peredri, Thomas Nagy, and Reinhold Kainhofer.
 *
 * © 2007-2010 Ian Cameron Smith <johantheghost@yahoo.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.substrate;


/**
 * Interface defining a colour palette.
 * 
 * <p>This class basically sets up a ZenGarden object and lets it run.
 */
public interface Palette {

    /**
     * Get a random colour from this palette.
     * 
     * @return          A radomly selected colour.
     */
    public int getRandom();

}

