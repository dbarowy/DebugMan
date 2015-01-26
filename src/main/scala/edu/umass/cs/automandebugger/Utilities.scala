package edu.umass.cs.plasma.automandebugger

object Utilities {
  def dateToTimestamp(d: java.util.Date) : Long = {
    d.getTime() / 1000
  }
}
