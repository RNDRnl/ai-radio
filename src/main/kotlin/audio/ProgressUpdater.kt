package audio

interface ProgressUpdater {
    fun makeCurrent(current: Boolean)
    fun updateProgress()
}
