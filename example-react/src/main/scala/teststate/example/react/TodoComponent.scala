package teststate.example.react

import monocle.macros.Lenses
import monocle.Lens
import japgolly.scalajs.react._, vdom.prefix_<^._

object TodoComponent {

  @Lenses
  case class TodoItem(text: String, completed: Boolean)

  @Lenses
  case class State(newItemText  : String,
                   items        : Vector[TodoItem],
                   showCompleted: Boolean) {

    def hideCompleted = !showCompleted
  }

  object State {
    def item(idx: Int): Lens[State, TodoItem] =
      items ^|-> atVectorIndex(idx)
  }

  def atVectorIndex[A](idx: Int) =
    Lens[Vector[A], A](_ apply idx)(a => _.updated(idx, a))

  private val newFormMarker =
    ReactAttr.devOnly("data-new-form") := 1

  final class Backend($: BackendScope[Unit, State]) {

    def render(s: State): ReactElement = {

      val items =
        if (s.showCompleted)
          s.items
        else
          s.items.iterator.filterNot(_.completed)

      <.div(
        renderNew(s.newItemText),
        renderList(items),
        renderToggleShowCompleted(s.showCompleted),
        renderSummary(s.items))
    }

    private def renderNew(text: String) = {
      // Prevent saving blank todos
      val trimmed: Option[String] =
        Some(text.trim).filter(_.nonEmpty)

      val save: Option[Callback] =
        trimmed map createNewItem

      <.div(
        newFormMarker,
        <.input.text(
          ^.value := text,
          ^.onChange ==> updateNewText),
        <.button(
          ^.`type` := "submit",
          ^.disabled := save.isEmpty,
          ^.onClick -->? save,
          "Add"))
    }

    private def renderList(items: TraversableOnce[TodoItem]): TagMod =
      if (items.isEmpty)
        EmptyTag
      else
        <.div(
          "Todo List",
          <.ul(
            items.toIterator.zipWithIndex.map { case (item, idx) =>
              <.li(
                ^.key := idx,
                renderItem(item, idx))
            }))

    private def renderItem(item: TodoItem, idx: Int) = {
      val complete =
        if (item.completed)
          EmptyTag
        else
          <.button(
            ^.onClick --> completeItem(idx),
            "Complete")

      <.div(
        <.pre(item.text),
        complete)
    }

    private def renderToggleShowCompleted(showCompleted: Boolean) =
      <.label(
        <.input.checkbox(
          ^.checked := showCompleted,
          ^.onChange --> $.modState(State.showCompleted set !showCompleted)),
        "Show completed.")

    private def renderSummary(items: Vector[TodoItem]) = {
      val total    = items.length
      val complete = items.count(_.completed)
      val pending  = total - complete

      def row(title: String, n: Int) =
        <.tr(
          <.td(title + ":"),
          <.td(n))

      <.table(
        <.tbody(
          row("Complete", complete),
          row("Pending" , pending),
          row("Total"   , total)))
    }

    private def updateNewText(ev: ReactEventI): Callback =
      ev.extract(_.target.value)(text =>
        $.modState(State.newItemText set text))

    private def createNewItem(text: String): Callback =
      $.modState(
        State.newItemText.set("") compose
        State.items.modify(_ :+ TodoItem(text, false)))

    private def completeItem(idx: Int): Callback =
      $.modState(
        (State.item(idx) ^|-> TodoItem.completed) set true)
  }

  val Component = ReactComponentB[Unit]("Todo Example")
    .initialState(State("", Vector.empty, false))
    .renderBackend[Backend]
    .build
}

