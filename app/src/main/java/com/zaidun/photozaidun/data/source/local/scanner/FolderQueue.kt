package com.zaidun.photozaidun.data.source.local.scanner

import java.util.ArrayDeque

class FolderQueue {

    private val queue = ArrayDeque<String>()

    fun enqueue(documentId: String) {
        queue.addLast(documentId)
    }

    fun dequeue(): String {
        return queue.removeFirst()
    }

    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }

    fun clear() {
        queue.clear()
    }
}