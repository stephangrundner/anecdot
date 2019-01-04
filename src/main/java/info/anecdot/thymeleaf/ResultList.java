package info.anecdot.thymeleaf;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.domain.Page;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * @author Stephan Grundner
 */
public class ResultList<T> implements List<T> {

    private final Page<T> page;

    public Page<T> getPage() {
        return page;
    }

    @Override
    public int size() {
        return page.getContent().size();
    }

    @Override
    public boolean isEmpty() {
        return page.getContent().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return page.getContent().contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return page.getContent().iterator();
    }

    @Override
    public Object[] toArray() {
        return page.getContent().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return page.getContent().toArray(a);
    }

    @Override
    public boolean add(T t) {
        return page.getContent().add(t);
    }

    @Override
    public boolean remove(Object o) {
        return page.getContent().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return page.getContent().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return page.getContent().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return page.getContent().addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return page.getContent().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return page.getContent().retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        page.getContent().replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        page.getContent().sort(c);
    }

    @Override
    public void clear() {
        page.getContent().clear();
    }

    @Override
    public boolean equals(Object o) {
        return page.getContent().equals(o);
    }

    @Override
    public T get(int index) {
        return page.getContent().get(index);
    }

    @Override
    public T set(int index, T element) {
        return page.getContent().set(index, element);
    }

    @Override
    public void add(int index, T element) {
        page.getContent().add(index, element);
    }

    @Override
    public T remove(int index) {
        return page.getContent().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return page.getContent().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return page.getContent().lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return page.getContent().listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return page.getContent().listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return page.getContent().subList(fromIndex, toIndex);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(page.getContent().hashCode())
                .append(page.hashCode())
                .toHashCode();
    }

    public ResultList(Page<T> page) {
        this.page = page;
    }
}
