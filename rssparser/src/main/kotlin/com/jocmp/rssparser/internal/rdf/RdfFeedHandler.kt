package com.jocmp.rssparser.internal.rdf

import com.jocmp.rssparser.internal.ChannelFactory
import com.jocmp.rssparser.internal.FeedHandler
import com.jocmp.rssparser.internal.rdf.RdfKeyword.Channel
import com.jocmp.rssparser.internal.rdf.RdfKeyword.Item
import com.jocmp.rssparser.model.RssChannel
import org.jsoup.nodes.Element

internal class RdfFeedHandler(val rdf: Element) : FeedHandler {
    private var channelFactory = ChannelFactory()

    override fun build(): RssChannel {
        rdf.children().forEach { node ->
            when (node.tagName()) {
                Channel.value -> channel(node)
                Item.Tag.value -> item(node)
            }
        }

        return channelFactory.build()
    }

    private fun channel(channel: Element) {
        channel.children().forEach { node ->
            when (node.tagName()) {
                Channel.Title.value -> channelFactory.channelBuilder.title(
                    node.wholeText().trim()
                )

                Channel.Image.Tag.value -> {
                    channelFactory.channelImageBuilder.url(node.attr(Channel.Image.ResourceAttribute.value))
                }

                Channel.DCDate.value -> channelFactory.channelBuilder.lastBuildDate(node.text())
                Channel.Description.value -> channelFactory.channelBuilder.description(
                    node.text().trim()
                )

                Channel.Link.value -> channelFactory.channelBuilder.link(node.text())
            }
        }
    }

    private fun item(item: Element) {
        item.children().forEach { node ->
            when (node.tagName()) {
                Item.Title.value -> channelFactory.articleBuilder.title(node.wholeText().trim())
                Item.Link.value -> channelFactory.articleBuilder.link(node.text())
                Item.DCDate.value -> channelFactory.articleBuilder.pubDate(node.text())
                Item.DCCreator.value -> channelFactory.articleBuilder.author(node.text())
                Item.Description.value -> {
                    channelFactory.articleBuilder.description(node.wholeText().trim())
                }
                Item.SlashSection.value -> channelFactory.articleBuilder.addCategory(node.text())
            }
        }

        channelFactory.buildArticle()
    }
}
