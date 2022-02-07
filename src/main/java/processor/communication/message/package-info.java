/**
 * This package includes data types for communication between server, worker and
 * remote controller.
 *
 * The name of messages starts with "Message". A name shows the sender and
 * receiver of message. There are five possible pairs of sender-receiver:
 * "SW","WS","WW","RS" and "SR", where "S" refers to server, "W" refers to
 * worker and "R" refers to remote controller.
 *
 * A message can contain any number of variables. Normally a variable is of a
 * primitive Java data type. For any user-defined data type or an arraylist, we
 * should define classes such that the values in the type/arraylist can be of
 * primitive data type. the Some messages contain arraylists of items. In this
 * package, those classes start with "Serializable" in their names. The
 * Serializable data types can be nested. Note that the max number of levels in
 * the hierarchy should be less than the number of delimiter types. See
 * "appendFieldDelimiter" in "MessageUtil".
 */
package processor.communication.message;