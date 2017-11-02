package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import static seedu.address.logic.commands.CopyCommand.CHOICE_ADDRESS;
import static seedu.address.logic.commands.CopyCommand.CHOICE_EMAIL;
import static seedu.address.logic.commands.CopyCommand.CHOICE_NAME;
import static seedu.address.logic.commands.CopyCommand.CHOICE_PHONE;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static seedu.address.logic.commands.SortCommand.SAVE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.ComponentManager;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.events.model.AddressBookChangedEvent;
import seedu.address.model.person.Person;
import seedu.address.model.person.ReadOnlyPerson;
import seedu.address.model.person.UniquePersonList;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.model.tag.Tag;

/**
 * Represents the in-memory model of the address book data.
 * All changes to any model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;
    private final FilteredList<ReadOnlyPerson> filteredPersons;
    private final Toolkit toolkit;
    private final Clipboard clipboard;

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, UserPrefs userPrefs) {
        super();
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
        this.toolkit = Toolkit.getDefaultToolkit();
        this.clipboard = toolkit.getSystemClipboard();
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    @Override
    public void resetData(ReadOnlyAddressBook newData) {
        addressBook.resetData(newData);
        indicateAddressBookChanged();
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    /**
     * Raises an event to indicate the model has changed
     */
    private void indicateAddressBookChanged() {
        raise(new AddressBookChangedEvent(addressBook));
    }

    @Override
    public synchronized void deletePerson(ReadOnlyPerson target) throws PersonNotFoundException {
        addressBook.removePerson(target);
        indicateAddressBookChanged();
    }

    @Override
    //Note FilteredList is unmodifiable hence sorting is done on internal list.
    public synchronized void sortAddressBook(int option, int saveOption)throws UniquePersonList.AddressBookIsEmpty {
        addressBook.sort(option);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        if (saveOption == SAVE) {
            indicateAddressBookChanged();
        }
    }

    @Override
    public synchronized void addPerson(ReadOnlyPerson person) throws DuplicatePersonException {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        indicateAddressBookChanged();
    }

    @Override
    public void copy(ReadOnlyPerson person, String choice) {
        Person target = new Person(person);
        StringSelection sel;

        switch (choice) {
        case CHOICE_NAME:
            sel = new StringSelection(target.getName().fullName);
            clipboard.setContents(sel, null);
            break;

        case CHOICE_PHONE:
            sel = new StringSelection(target.getPhone().value);
            clipboard.setContents(sel, null);
            break;

        case CHOICE_EMAIL:
            sel = new StringSelection(target.getEmail().value);
            clipboard.setContents(sel, null);
            break;

        case CHOICE_ADDRESS:
            sel = new StringSelection(target.getAddress().value);
            clipboard.setContents(sel, null);
            break;

        default:
            break;
        }
    }

    @Override
    public void updatePerson(ReadOnlyPerson target, ReadOnlyPerson editedPerson)
            throws DuplicatePersonException, PersonNotFoundException {
        requireAllNonNull(target, editedPerson);

        addressBook.updatePerson(target, editedPerson);
        indicateAddressBookChanged();
    }

    @Override
    public List<Tag> getTagsList(){
        List<Tag> unsortedListOfTags = new ArrayList<>();

        filteredPersons.forEach(persons -> unsortedListOfTags.addAll(persons.getTags()));

        //Removes duplicate tags to ensure all tags are unique
        List<Tag> listOfTags= unsortedListOfTags.stream().distinct().collect(Collectors.toList());
        //Sorts tags in alphabetical order
        listOfTags.sort(Comparator.comparing(Tag::getTagName));

        return listOfTags;
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code ReadOnlyPerson} backed by the internal list of
     * {@code addressBook}
     */
    @Override
    public ObservableList<ReadOnlyPerson> getFilteredPersonList() {
        return FXCollections.unmodifiableObservableList(filteredPersons);
    }

    @Override
    public void updateFilteredPersonList(Predicate<ReadOnlyPerson> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManager)) {
            return false;
        }

        // state check
        ModelManager other = (ModelManager) obj;
        return addressBook.equals(other.addressBook)
                && filteredPersons.equals(other.filteredPersons);
    }

}
