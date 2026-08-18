import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import Button from './Button';

describe('Button', () => {
  it('renders its children', () => {
    render(<Button>Đăng nhập</Button>);
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeInTheDocument();
  });

  it('is disabled and shows a spinner when loading', () => {
    const { container } = render(<Button loading>Đăng nhập</Button>);
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeDisabled();
    expect(container.querySelector('.btn-spinner')).toBeInTheDocument();
  });
});
