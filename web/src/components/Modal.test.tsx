import '@testing-library/jest-dom';
import { fireEvent, render, screen } from '@testing-library/react';
import Modal from './Modal';

describe('Modal', () => {
  it('renders nothing when closed', () => {
    render(
      <Modal open={false} onClose={() => {}}>
        content
      </Modal>,
    );
    expect(screen.queryByText('content')).not.toBeInTheDocument();
  });

  it('renders title and children when open', () => {
    render(
      <Modal open onClose={() => {}} title="Xóa tài khoản">
        Nội dung
      </Modal>,
    );
    expect(screen.getByText('Xóa tài khoản')).toBeInTheDocument();
    expect(screen.getByText('Nội dung')).toBeInTheDocument();
  });

  it('calls onClose when Escape is pressed', () => {
    const onClose = jest.fn();
    render(
      <Modal open onClose={onClose}>
        content
      </Modal>,
    );
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when the overlay is clicked', () => {
    const onClose = jest.fn();
    render(
      <Modal open onClose={onClose}>
        content
      </Modal>,
    );
    fireEvent.click(document.querySelector('.modal-overlay') as HTMLElement);
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
